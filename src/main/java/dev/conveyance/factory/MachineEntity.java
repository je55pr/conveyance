package dev.conveyance.factory;

import dev.conveyance.ItemPolicy;
import dev.conveyance.player.ResearchState;
import dev.conveyance.player.Technology;
import dev.conveyance.player.InventoryAccess;
import dev.conveyance.pollution.PollutionData;
import dev.conveyance.pollution.PollutionRates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class MachineEntity extends PoweredMachineEntity implements Container, MenuProvider, PowerProducer, FluidEndpoint {
    public static final int SOLAR_PEAK_KW = 60;
    public enum MiningStatus {
        MINING, NO_DEPOSIT, NO_FUEL, NO_OUTPUT, OUTPUT_BLOCKED;
        public Component label() { return Component.translatable("status.conveyance.miner." + name().toLowerCase(java.util.Locale.ROOT)); }
        public Component description() { return Component.translatable("status.conveyance.miner." + name().toLowerCase(java.util.Locale.ROOT) + ".description"); }
    }
    private NonNullList<ItemStack> items;
    private ResourceLocation selectedRecipe, smeltingRecipe;
    private boolean creativeRecipe;
    private int cookingTime = 64, cursor, labResearch;
    private final BurnerFuel burner = new BurnerFuel(0);
    private double burnerStep;
    private double coalWork;
    private ItemStack pendingOre = ItemStack.EMPTY;
    private final DrillMotion drill = new DrillMotion(this);
    private final UtilityMachine utility = new UtilityMachine(this);
    public UtilityMachine utility() { return utility; }
    private final SteamMachine steam;
    private final ChemicalMachine chemical;
    private final dev.conveyance.combat.TurretLogic turret = new dev.conveyance.combat.TurretLogic(this);
    public dev.conveyance.combat.TurretLogic turret() { return turret; }
    private final dev.conveyance.combat.LaserTurretLogic laser = new dev.conveyance.combat.LaserTurretLogic(this);
    public dev.conveyance.combat.LaserTurretLogic laser() { return laser; }
    @Override public double powerDemand() { return kind() == MachineKind.LASER_TURRET ? laser.demandKW() : super.powerDemand(); }
    public ChemicalMachine chemical() { return chemical; }
    public SteamMachine steam() { return steam; }
    @Override public java.util.List<FluidEndpoint.Port> fluidPorts() { return kind().utility() ? utility.ports() : kind().chemical() ? chemical.ports() : steam.ports(); }
    @Override public FluidTank fluidTank(int channel) { return kind().utility() ? utility.tank(channel) : kind().chemical() ? chemical.tank(channel) : steam.tank(channel); }
    public DrillMotion drillMotion() { return drill; }

    public MachineEntity(BlockPos pos, BlockState state) {
        super(FactoryContent.MACHINE_ENTITY.get(), pos, state,
                MachineBlock.kind(state).powerKW, MachineBlock.kind(state).cycleTicks);
        items = NonNullList.withSize(kind().slots, ItemStack.EMPTY);
        steam = new SteamMachine(this);
        chemical = new ChemicalMachine(this);
    }
    public MachineKind kind() { return MachineBlock.kind(getBlockState()); }
    public int outputSlot() { return kind().furnace() ? 2 : kind().assembler() ? 5 : kind() == MachineKind.REFINERY || kind() == MachineKind.CHEMICAL_PLANT ? 2 : -1; }
    public int fuelRemaining() { return burner.remainingTicks(kind().burnerKW()); }
    public double fuelReserveMJ() { return burner.reserveMJ(); }
    @Override protected double idlePower() { return kind().idleKW(); }
    public int automaticInputLimit(int slot, ItemStack stack) {
        return switch (kind()) {
            case ASSEMBLER, ASSEMBLER_2 -> {
                CraftingRecipe recipe = level == null ? null : selectedRecipe();
                var requirements = recipe == null ? java.util.List.<AssemblyRecipes.Requirement>of() : AssemblyRecipes.requirements(recipe);
                yield slot < requirements.size() ? requirements.get(slot).count() * 2 : 0;
            }
            case FURNACE, STEEL_FURNACE -> {
                var recipe = slot == 0 ? IndustrialSmelting.find(level, stack) : null;
                yield slot == 0 ? recipe == null ? 0 : 2 * IndustrialSmelting.inputCount(recipe) : slot == 1 ? ItemTransfer.smallBuffer(stack) : 0;
            }
            case BURNER_MINER, LAB, BOILER -> ItemTransfer.smallBuffer(stack);
            case SOLAR_PANEL, OFFSHORE_PUMP, STEAM_ENGINE, STORAGE_TANK, FLUID_PUMP, ACCUMULATOR, LASER_TURRET -> 0;
            case PUMPJACK, REFINERY, CHEMICAL_PLANT -> chemical.inputLimit(slot);
            case GUN_TURRET -> 10;
        };
    }
    public int progressPixels() { return (int) (24 * (kind() == MachineKind.LAB ? currentWork() / workDuration() : kind().assembler() ? currentWork() / workDuration() : coalWork / (kind() == MachineKind.BURNER_MINER ? 80 : cookingTime))); }
    @Override protected int workDuration() {
        if (kind() == MachineKind.LAB && level != null) { var tech = Technology.byId(ResearchState.selected(level)); return tech == null ? 20 : Math.max(1, tech.unitTicks); }
        if (kind().assembler() && level != null && selectedRecipe() instanceof FactoryCraftingRecipe recipe) return Math.max(1, (int) Math.round(recipe.craftingTicks() / kind().craftingSpeed()));
        return super.workDuration();
    }
    public MiningStatus miningStatus() {
        if (level == null || kind() != MachineKind.BURNER_MINER) return MiningStatus.NO_DEPOSIT;
        var facing = getBlockState().getValue(MachineBlock.FACING);
        var resource = MiningResources.find(level, 4, cursor, part -> MachineBlock.partPosition(worldPosition, facing, kind(), part).below());
        if (resource.stack().isEmpty()) return MiningStatus.NO_DEPOSIT;
        if (!burner.available(items.get(0))) return MiningStatus.NO_FUEL;
        BlockPos output = worldPosition.relative(facing);
        ConveyorEntity lower = ConveyorEntity.at(level, output.below());
        if (ItemTransfer.containerAt(level, output) == null && ConveyorEntity.at(level, output) == null
                && (lower == null || !lower.slope()) && JunctionEntity.at(level, output) == null)
            return MiningStatus.NO_OUTPUT;
        return ItemTransfer.canOutput(level, output, facing.getOpposite(), resource.stack(), true) ? MiningStatus.MINING : MiningStatus.OUTPUT_BLOCKED;
    }

    @Override public double powerDistanceSquared(BlockPos pole) {
        BlockPos far = MachineBlock.partPosition(worldPosition, getBlockState().getValue(MachineBlock.FACING), kind(), kind().parts() - 1);
        int x = Math.max(Math.min(worldPosition.getX(), far.getX()), Math.min(Math.max(worldPosition.getX(), far.getX()), pole.getX()));
        int y = Math.max(worldPosition.getY(), Math.min(far.getY(), pole.getY()));
        int z = Math.max(Math.min(worldPosition.getZ(), far.getZ()), Math.min(Math.max(worldPosition.getZ(), far.getZ()), pole.getZ()));
        return pole.distSqr(new BlockPos(x, y, z));
    }
    public static void tick(Level level, BlockPos pos, BlockState state, MachineEntity machine) {
        if (machine.kind() == MachineKind.SOLAR_PANEL && level.getGameTime() % 20 != 0) return;
        machine.burnerStep = 0;
        switch (machine.kind()) {
            case BURNER_MINER -> machine.mine();
            case FURNACE, STEEL_FURNACE -> machine.smelt();
            case ASSEMBLER, ASSEMBLER_2 -> machine.assemble();
            case LAB -> machine.research();
            case SOLAR_PANEL -> {}
            case STORAGE_TANK, FLUID_PUMP, ACCUMULATOR -> machine.utility.tick();
            case BOILER, STEAM_ENGINE, OFFSHORE_PUMP -> machine.steam.tick();
            case PUMPJACK, REFINERY, CHEMICAL_PLANT -> machine.chemical.tick();
            case GUN_TURRET -> machine.turret.tick();
            case LASER_TURRET -> machine.laser.tick();
        }
        boolean lit = machine.kind().chemical() ? machine.chemical.status() == ChemicalMachine.Status.WORKING : machine.kind().fluids() ? machine.steam.status() == SteamMachine.Status.WORKING : machine.kind() == MachineKind.SOLAR_PANEL ? machine.outputPower() > 0 : machine.kind() == MachineKind.LAB
                ? machine.hasScience(Technology.byId(ResearchState.selected(level))) && PowerNetwork.fraction(level, pos) > 0
                : machine.kind() == MachineKind.LASER_TURRET ? machine.laser.energyMJ() + .000001 >= dev.conveyance.combat.LaserTurretLogic.SHOT_MJ : machine.burnerStep > 0;
        if (state.getValue(MachineBlock.LIT) != lit || level.getGameTime() % 20 == 0) {
            var facing = state.getValue(MachineBlock.FACING);
            for (int part = 0; part < machine.kind().parts(); part++) {
                BlockPos target = MachineBlock.partPosition(pos, facing, machine.kind(), part);
                if (!level.hasChunkAt(target)) continue;
                BlockState current = level.getBlockState(target);
                if (MachineBlock.kind(current) == machine.kind() && current.getValue(MachineBlock.PART) == part
                        && current.getValue(MachineBlock.FACING) == facing && current.getValue(MachineBlock.LIT) != lit)
                    level.setBlockAndUpdate(target, current.setValue(MachineBlock.LIT, lit));
            }
        }
    }
    private boolean burn(int slot) {
        burnerStep = burner.consume(level, items.get(slot), kind().burnerKW());
        if (burnerStep <= 0) return false;
        PollutionData.emit(level, worldPosition, PollutionRates.of(getBlockState().getBlock()) * burnerStep);
        setChanged(); return true;
    }
    private void mine() {
        var facing = getBlockState().getValue(MachineBlock.FACING);
        if (!pendingOre.isEmpty()) {
            if (ItemTransfer.output(level, worldPosition.relative(facing), facing.getOpposite(), pendingOre.copyWithCount(1), true)) { pendingOre.shrink(1); setChanged(); }
            return;
        }
        var resource = MiningResources.find(level, 4, cursor, part -> MachineBlock.partPosition(worldPosition, facing, kind(), part).below());
        if (resource.stack().isEmpty() || !ItemTransfer.canOutput(level, worldPosition.relative(facing), facing.getOpposite(), resource.stack(), true) || !burn(0)) { drill.capture(0); return; }
        drill.capture(12 * ResearchState.miningSpeed(level) * burnerStep);
        coalWork += ResearchState.miningSpeed(level) * burnerStep;
        if (coalWork + 0.000001 < kind().cycleTicks) return;
        if (ItemTransfer.output(level, worldPosition.relative(facing), facing.getOpposite(), resource.stack(), true)) {
            coalWork = Math.max(0, coalWork - kind().cycleTicks);
            if (modules.produce("mining", ResearchState.miningProductivity(level)) > 0) pendingOre = resource.stack().copy();
            cursor = resource.nextCursor();
            setChanged();
        }
    }
    private void smelt() {
        var recipe = IndustrialSmelting.find(level, items.get(0));
        if (recipe == null) { coalWork = 0; return; }
        if (!recipe.getId().equals(smeltingRecipe)) { smeltingRecipe = recipe.getId(); coalWork = 0; setChanged(); }
        cookingTime = Math.max(1, IndustrialSmelting.cookingTime(recipe));
        int required = IndustrialSmelting.inputCount(recipe);
        ItemStack result = recipe.assemble(new SimpleContainer(items.get(0)), level.registryAccess());
        if (items.get(0).getCount() < required || !fitsOutput(result) || !burn(1)) return;
        coalWork += kind().smeltingSpeed() * burnerStep;
        if (coalWork + 0.000001 < cookingTime) return;
        items.get(0).shrink(required);
        putOutput(result);
        ResearchState.recordCraft(level, result);
        coalWork = Math.max(0, coalWork - cookingTime);
        setChanged();
    }
    public CraftingRecipe selectedRecipe() {
        if (selectedRecipe == null) return null;
        if (!creativeRecipe && !ResearchState.recipeUnlocked(selectedRecipe, ResearchState.mask(level))) return null;
        var recipe = level.getRecipeManager().byKey(selectedRecipe).orElse(null);
        return recipe instanceof CraftingRecipe crafting ? crafting : null;
    }
    public int selectedRecipeIndex() {
        CraftingRecipe selected = selectedRecipe();
        var recipes = AssemblyRecipes.all(level);
        for (int index = 0; index < recipes.size(); index++) if (selected != null && recipes.get(index).getId().equals(selected.getId())) return index;
        return -1;
    }
    public boolean selectRecipe(ResourceLocation id, boolean creative) {
        if (!kind().assembler() || level == null
                || !creative && !ResearchState.recipeUnlocked(id, ResearchState.mask(level))
                || AssemblyRecipes.all(level).stream().noneMatch(recipe -> recipe.getId().equals(id))) return false;
        boolean allowLocked = creative && !ResearchState.recipeUnlocked(id, ResearchState.mask(level));
        if (id.equals(selectedRecipe) && creativeRecipe == allowLocked) return true;
        selectedRecipe = id;
        creativeRecipe = allowLocked;
        resetWork();
        return true;
    }
    public boolean selectRecipe(Player player, ResourceLocation id) {
        if (!kind().assembler() || level == null || player.isSpectator() || !stillValid(player)) return false;
        CraftingRecipe recipe = AssemblyRecipes.all(level).stream().filter(candidate -> candidate.getId().equals(id)).findFirst().orElse(null);
        if (recipe == null || !player.isCreative() && !ResearchState.recipeUnlocked(id, ResearchState.mask(level))) return false;
        var requirements = AssemblyRecipes.requirements(recipe);
        var extra = InventoryAccess.extra(player);
        var inventory = InventoryAccess.snapshot(player, extra);
        var returned = new java.util.ArrayList<Integer>();
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stored = items.get(slot);
            boolean compatible = slot == outputSlot() ? ItemStack.isSameItemSameTags(stored, recipe.getResultItem(level.registryAccess()))
                    : slot < requirements.size() && requirements.get(slot).ingredient().test(stored);
            if (stored.isEmpty() || compatible) continue;
            if (!InventoryAccess.insert(inventory, stored)) { player.displayClientMessage(Component.translatable("message.conveyance.inventory_full"), true); return false; }
            returned.add(slot);
        }
        if (!selectRecipe(id, player.isCreative())) return false;
        returned.forEach(slot -> items.set(slot, ItemStack.EMPTY));
        if (!returned.isEmpty()) { InventoryAccess.commit(player, extra, inventory); setChanged(); }
        return true;
    }
    @Override protected boolean hasWork() {
        if (level == null) return false;
        return switch (kind()) {
            case ASSEMBLER, ASSEMBLER_2 -> runnableAssembly() != null;
            case FLUID_PUMP -> utility.hasWork();
            case LAB -> hasScience(Technology.byId(ResearchState.selected(level)));
            case PUMPJACK, REFINERY, CHEMICAL_PLANT -> chemical.hasWork();
            default -> false;
        };
    }
    private CraftingRecipe runnableAssembly() {
        CraftingRecipe recipe = selectedRecipe();
        if (recipe == null) return null;
        var requirements = AssemblyRecipes.requirements(recipe);
        if (requirements.size() > 5 || requirements.isEmpty()) return null;
        for (int slot = 0; slot < requirements.size(); slot++) {
            var requirement = requirements.get(slot);
            if (!requirement.ingredient().test(items.get(slot)) || items.get(slot).getCount() < requirement.count()) return null;
        }
        ItemStack output = recipe.getResultItem(level.registryAccess()).copy();
        output.setCount(output.getCount() * (1 + modules.previewBonus(recipe.getId().toString(), modules.productivity(MachineModules.productive(recipe)))));
        return fitsOutput(output) ? recipe : null;
    }
    private void assemble() {
        CraftingRecipe recipe = runnableAssembly();
        if (recipe == null || !advanceWork()) return;
        var requirements = AssemblyRecipes.requirements(recipe);
        ItemStack result = recipe.getResultItem(level.registryAccess()).copy();
        result.setCount(result.getCount() * (1 + modules.produce(recipe.getId().toString(), modules.productivity(MachineModules.productive(recipe)))));
        for (int slot = 0; slot < requirements.size(); slot++) items.get(slot).shrink(requirements.get(slot).count());
        putOutput(result);
        ResearchState.recordCraft(level, result);
        setChanged();
    }
    private void research() {
        int selected = ResearchState.selected(level);
        if (selected != labResearch) { labResearch = selected; resetWork(); }
        Technology tech = Technology.byId(selected);
        if (!hasScience(tech) || !advanceWork()) return;
        if (ResearchState.applyScience(level, selected)) {
            for (int slot = 0; slot < Technology.SCIENCE_SLOTS; slot++) if (tech.needsPack(slot)) items.get(slot).shrink(1);
            int extra = modules.produce("research:" + tech.key, modules.productivity(true));
            for (int unit = 0; unit < extra; unit++) ResearchState.applyScience(level, selected);
            setChanged();
        }
    }
    private boolean hasScience(Technology tech) {
        if (tech == null) return false;
        for (int slot = 0; slot < Technology.SCIENCE_SLOTS; slot++) if (tech.needsPack(slot) && !items.get(slot).is(Technology.packItem(slot))) return false;
        return true;
    }
    @Override public double outputPower() {
        if (kind() == MachineKind.STEAM_ENGINE) return steam.outputKW();
        if (kind() != MachineKind.SOLAR_PANEL || level == null || !level.isDay()) return 0;
        int clear = 0;
        var facing = getBlockState().getValue(MachineBlock.FACING);
        for (int part = 0; part < kind().parts(); part++) if (level.canSeeSky(MachineBlock.partPosition(worldPosition, facing, kind(), part).above())) clear++;
        return (int) Math.round((double) SOLAR_PEAK_KW * clear / kind().parts() * Math.max(0, Math.cos(level.getSunAngle(0))));
    }
    @Override public void onLoad() {
        super.onLoad();
        if (kind() == MachineKind.ACCUMULATOR || kind() == MachineKind.SOLAR_PANEL || kind() == MachineKind.STEAM_ENGINE) PowerNetwork.register(level, worldPosition, PowerNetwork.Kind.GENERATOR);
        if (kind().fluids()) FluidNetwork.register(level, worldPosition);
    }
    @Override public double availablePower() { if (kind() == MachineKind.ACCUMULATOR) return utility.availablePower(); return kind() == MachineKind.STEAM_ENGINE ? steam.availablePower() : outputPower(); }
    @Override public boolean consumesFuelPower() { return kind() == MachineKind.STEAM_ENGINE; }
    @Override public void setRemoved() {
        if (kind() == MachineKind.ACCUMULATOR || kind() == MachineKind.SOLAR_PANEL || kind() == MachineKind.STEAM_ENGINE) PowerNetwork.unregister(level, worldPosition, PowerNetwork.Kind.GENERATOR);
        if (kind().fluids()) FluidNetwork.unregister(level, worldPosition);
        super.setRemoved();
    }
    private boolean fitsOutput(ItemStack result) {
        if (result.isEmpty() || ItemPolicy.isDisabled(result)) return false;
        ItemStack stored = items.get(outputSlot());
        return (stored.isEmpty() || ItemStack.isSameItemSameTags(stored, result))
                && stored.getCount() + result.getCount() <= Math.min(getMaxStackSize(), result.getMaxStackSize());
    }
    private void putOutput(ItemStack result) {
        int slot = outputSlot();
        if (items.get(slot).isEmpty()) items.set(slot, result.copy()); else items.get(slot).grow(result.getCount());
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (kind()) {
            case BURNER_MINER, BOILER -> GeneratorEntity.isFuel(stack);
            case FURNACE, STEEL_FURNACE -> slot == 1 ? GeneratorEntity.isFuel(stack) : slot == 0 && IndustrialSmelting.find(level, stack) != null;
            case ASSEMBLER, ASSEMBLER_2 -> {
                CraftingRecipe recipe = level == null ? null : selectedRecipe();
                var requirements = recipe == null ? java.util.List.<AssemblyRecipes.Requirement>of() : AssemblyRecipes.requirements(recipe);
                yield slot < requirements.size() && requirements.get(slot).ingredient().test(stack);
            }
            case LAB -> slot >= 0 && slot < Technology.SCIENCE_SLOTS && stack.is(Technology.packItem(slot));
            case PUMPJACK, REFINERY, CHEMICAL_PLANT -> chemical.accepts(slot, stack);
            case SOLAR_PANEL, OFFSHORE_PUMP, STEAM_ENGINE, STORAGE_TANK, FLUID_PUMP, ACCUMULATOR, LASER_TURRET -> false;
            case GUN_TURRET -> slot == 0 && dev.conveyance.combat.CombatShots.magazine(stack);
        };
    }
    @Override public boolean canTakeItem(Container destination, int slot, ItemStack stack) { return slot == outputSlot() || (kind() == MachineKind.BOILER || kind() == MachineKind.GUN_TURRET) && slot == 0; }
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int count) { ItemStack result = ContainerHelper.removeItem(items, slot, count); setChanged(); return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public boolean stillValid(Player player) { return !isRemoved() && (RemoteBuilding.canReach(player, worldPosition) || player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) < 100); }
    @Override public Component getDisplayName() { return Component.translatable("block.conveyance." + kind().id); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        if (kind() == MachineKind.SOLAR_PANEL || kind().utility()) return null;
        if (kind() == MachineKind.GUN_TURRET) return new dev.conveyance.combat.TurretMenu(id, inventory, this);
        if (kind() == MachineKind.LASER_TURRET) return new dev.conveyance.combat.LaserTurretMenu(id, inventory, this);
        if (kind().chemical()) return new ChemicalMenu(id, inventory, this);
        if (kind().fluids()) return new FluidMenu(id, inventory, this);
        return kind() == MachineKind.BURNER_MINER ? new GeneratorMenu(id, inventory, this)
                : kind() == MachineKind.LAB ? new GeneratorMenu(id, inventory, this, true) : new ProcessingMenu(id, inventory, this);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        if (kind().utility()) tag.put("Utility", utility.save());
        if (kind() == MachineKind.GUN_TURRET) turret.save(tag);
        if (kind() == MachineKind.LASER_TURRET) laser.save(tag);
        if (kind().chemical()) tag.put("ChemicalMachine", chemical.save());
        else if (kind().fluids()) tag.put("SteamMachine", steam.save());
        burner.save(tag); tag.putInt("BurnTicks", fuelRemaining()); tag.putDouble("CoalWork", coalWork); tag.putInt("Cursor", cursor);
        tag.putInt("LabResearch", labResearch); tag.put("PendingOre", pendingOre.save(new CompoundTag()));
        if (smeltingRecipe != null) tag.putString("SmeltingRecipe", smeltingRecipe.toString());
        if (selectedRecipe != null) {
            tag.putString("SelectedRecipe", selectedRecipe.toString());
        }
        tag.putBoolean("CreativeRecipe", creativeRecipe);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(kind().slots, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        if (kind().utility()) utility.load(tag.getCompound("Utility"));
        if (kind() == MachineKind.GUN_TURRET) turret.load(tag);
        if (kind() == MachineKind.LASER_TURRET) laser.load(tag);
        if (kind().chemical()) chemical.load(tag.getCompound("ChemicalMachine"));
        else if (kind().fluids()) steam.load(tag.getCompound("SteamMachine"));
        burner.load(tag);
        coalWork = Math.max(0, tag.getDouble("CoalWork")); cursor = Math.floorMod(tag.getInt("Cursor"), 4);
        labResearch = tag.getInt("LabResearch"); pendingOre = ItemStack.of(tag.getCompound("PendingOre"));
        selectedRecipe = tag.contains("SelectedRecipe") ? ResourceLocation.tryParse(tag.getString("SelectedRecipe")) : null;
        creativeRecipe = tag.getBoolean("CreativeRecipe");
        smeltingRecipe = tag.contains("SmeltingRecipe") ? ResourceLocation.tryParse(tag.getString("SmeltingRecipe")) : null;
        drill.read(tag);
    }
    @Override public CompoundTag getUpdateTag() { var tag = saveWithoutMetadata(); drill.write(tag); return tag; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public AABB getRenderBoundingBox() {
        BlockPos far = MachineBlock.partPosition(worldPosition, getBlockState().getValue(MachineBlock.FACING), kind(), kind().parts() - 1);
        return new AABB(Math.min(worldPosition.getX(), far.getX()), worldPosition.getY(), Math.min(worldPosition.getZ(), far.getZ()),
                Math.max(worldPosition.getX(), far.getX()) + 1, worldPosition.getY() + kind().height, Math.max(worldPosition.getZ(), far.getZ()) + 1);
    }
}
