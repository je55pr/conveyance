package dev.conveyance.factory;

import dev.conveyance.ItemPolicy;
import dev.conveyance.player.ResearchState;
import dev.conveyance.player.Technology;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

public final class MachineEntity extends PoweredMachineEntity implements Container, MenuProvider, PowerProducer {
    public static final int SOLAR_PEAK_KW = 60;
    public enum MiningStatus {
        MINING, NO_DEPOSIT, NO_FUEL, NO_OUTPUT, OUTPUT_BLOCKED;
        public Component label() { return Component.translatable("status.conveyance.miner." + name().toLowerCase(java.util.Locale.ROOT)); }
        public Component description() { return Component.translatable("status.conveyance.miner." + name().toLowerCase(java.util.Locale.ROOT) + ".description"); }
    }
    private NonNullList<ItemStack> items;
    private ResourceLocation selectedRecipe, smeltingRecipe;
    private boolean creativeRecipe;
    private int burnTicks, cookingTime = 64, cursor, labResearch;
    private double coalWork;

    public MachineEntity(BlockPos pos, BlockState state) {
        super(FactoryContent.MACHINE_ENTITY.get(), pos, state,
                MachineBlock.kind(state).powerKW, MachineBlock.kind(state).cycleTicks);
        items = NonNullList.withSize(kind().slots, ItemStack.EMPTY);
    }
    public MachineKind kind() { return MachineBlock.kind(getBlockState()); }
    public int outputSlot() { return kind() == MachineKind.BURNER_MINER || kind() == MachineKind.LAB || kind() == MachineKind.SOLAR_PANEL ? -1 : kind() == MachineKind.FURNACE ? 2 : 4; }
    public int fuelRemaining() { return burnTicks; }
    public int automaticInputLimit(int slot, ItemStack stack) {
        return switch (kind()) {
            case ASSEMBLER -> {
                CraftingRecipe recipe = level == null ? null : selectedRecipe();
                var requirements = recipe == null ? java.util.List.<AssemblyRecipes.Requirement>of() : AssemblyRecipes.requirements(recipe);
                yield slot < requirements.size() ? requirements.get(slot).count() * 2 : 0;
            }
            case FURNACE -> {
                var recipe = slot == 0 ? IndustrialSmelting.find(level, stack) : null;
                yield slot == 0 ? recipe == null ? 0 : 2 * IndustrialSmelting.inputCount(recipe) : slot == 1 ? ItemTransfer.smallBuffer(stack) : 0;
            }
            case BURNER_MINER, LAB -> ItemTransfer.smallBuffer(stack);
            case SOLAR_PANEL -> 0;
        };
    }
    public int progressPixels() { return (int) (24 * (kind() == MachineKind.LAB ? currentWork() / 60 : kind() == MachineKind.ASSEMBLER ? currentWork() / 40 : coalWork / (kind() == MachineKind.BURNER_MINER ? 40 : cookingTime))); }
    public MiningStatus miningStatus() {
        if (level == null || kind() != MachineKind.BURNER_MINER) return MiningStatus.NO_DEPOSIT;
        var facing = getBlockState().getValue(MachineBlock.FACING);
        var resource = MiningResources.find(level, 4, cursor, part -> MachineBlock.partPosition(worldPosition, facing, kind(), part).below());
        if (resource.stack().isEmpty()) return MiningStatus.NO_DEPOSIT;
        if (burnTicks == 0 && !GeneratorEntity.isFuel(items.get(0))) return MiningStatus.NO_FUEL;
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
        switch (machine.kind()) {
            case BURNER_MINER -> machine.mine();
            case FURNACE -> machine.smelt();
            case ASSEMBLER -> machine.assemble();
            case LAB -> machine.research();
            case SOLAR_PANEL -> {}
        }
        boolean lit = machine.kind() == MachineKind.SOLAR_PANEL ? machine.outputPower() > 0 : machine.kind() == MachineKind.LAB
                ? machine.hasScience(Technology.byId(ResearchState.selected(level))) && PowerNetwork.fraction(level, pos) > 0
                : machine.kind() != MachineKind.ASSEMBLER && machine.burnTicks > 0;
        if (state.getValue(MachineBlock.LIT) != lit) level.setBlockAndUpdate(pos, state.setValue(MachineBlock.LIT, lit));
    }
    private boolean burn(int slot) {
        if (burnTicks == 0) {
            if (!GeneratorEntity.isFuel(items.get(slot))) return false;
            burnTicks = GeneratorEntity.fuelTicks(items.get(slot), level);
            items.get(slot).shrink(1);
            setChanged();
        }
        burnTicks--;
        if (level.getGameTime() % 20 == 0) setChanged();
        return true;
    }
    private void mine() {
        var facing = getBlockState().getValue(MachineBlock.FACING);
        var resource = MiningResources.find(level, 4, cursor, part -> MachineBlock.partPosition(worldPosition, facing, kind(), part).below());
        if (resource.stack().isEmpty() || !ItemTransfer.canOutput(level, worldPosition.relative(facing), facing.getOpposite(), resource.stack(), true) || !burn(0)) return;
        coalWork += ResearchState.miningSpeed(level);
        if (coalWork + 0.000001 < kind().cycleTicks) return;
        if (ItemTransfer.output(level, worldPosition.relative(facing), facing.getOpposite(), resource.stack(), true)) {
            coalWork = Math.max(0, coalWork - kind().cycleTicks);
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
        coalWork += ResearchState.smeltingSpeed(level);
        if (coalWork + 0.000001 < cookingTime) return;
        items.get(0).shrink(required);
        putOutput(result);
        coalWork = Math.max(0, coalWork - cookingTime);
        setChanged();
    }
    public CraftingRecipe selectedRecipe() {
        if (selectedRecipe == null) {
            var recipes = AssemblyRecipes.available(level);
            if (recipes.isEmpty()) return null;
            selectedRecipe = recipes.stream().filter(recipe -> recipe.getId().getPath().equals("conveyor")).findFirst().orElse(recipes.get(0)).getId();
            setChanged();
        }
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
        if (kind() != MachineKind.ASSEMBLER || level == null
                || !creative && !ResearchState.recipeUnlocked(id, ResearchState.mask(level))
                || AssemblyRecipes.all(level).stream().noneMatch(recipe -> recipe.getId().equals(id))) return false;
        boolean allowLocked = creative && !ResearchState.recipeUnlocked(id, ResearchState.mask(level));
        if (id.equals(selectedRecipe) && creativeRecipe == allowLocked) return true;
        selectedRecipe = id;
        creativeRecipe = allowLocked;
        resetWork();
        return true;
    }
    @Override protected boolean hasWork() {
        if (level == null) return false;
        return switch (kind()) {
            case ASSEMBLER -> runnableAssembly() != null;
            case LAB -> hasScience(Technology.byId(ResearchState.selected(level)));
            default -> false;
        };
    }
    private CraftingRecipe runnableAssembly() {
        CraftingRecipe recipe = selectedRecipe();
        if (recipe == null) return null;
        var requirements = AssemblyRecipes.requirements(recipe);
        if (requirements.size() > 4 || requirements.isEmpty()) return null;
        for (int slot = 0; slot < requirements.size(); slot++) {
            var requirement = requirements.get(slot);
            if (!requirement.ingredient().test(items.get(slot)) || items.get(slot).getCount() < requirement.count()) return null;
        }
        return fitsOutput(recipe.getResultItem(level.registryAccess())) ? recipe : null;
    }
    private void assemble() {
        CraftingRecipe recipe = runnableAssembly();
        if (recipe == null || !advanceWork()) return;
        var requirements = AssemblyRecipes.requirements(recipe);
        ItemStack result = recipe.getResultItem(level.registryAccess());
        for (int slot = 0; slot < requirements.size(); slot++) items.get(slot).shrink(requirements.get(slot).count());
        putOutput(result);
        setChanged();
    }
    private void research() {
        int selected = ResearchState.selected(level);
        if (selected != labResearch) { labResearch = selected; resetWork(); }
        Technology tech = Technology.byId(selected);
        if (!hasScience(tech) || !advanceWork()) return;
        if (ResearchState.applyScience(level, selected)) {
            items.get(0).shrink(1);
            if (tech.green) items.get(1).shrink(1);
            setChanged();
        }
    }
    private boolean hasScience(Technology tech) {
        return tech != null && items.get(0).is(FactoryContent.RED_SCIENCE_ITEM.get())
                && (!tech.green || items.get(1).is(FactoryContent.GREEN_SCIENCE_ITEM.get()));
    }
    @Override public int outputPower() {
        if (kind() != MachineKind.SOLAR_PANEL || level == null || !level.isDay()) return 0;
        int clear = 0;
        var facing = getBlockState().getValue(MachineBlock.FACING);
        for (int part = 0; part < kind().parts(); part++) if (level.canSeeSky(MachineBlock.partPosition(worldPosition, facing, kind(), part).above())) clear++;
        return (int) Math.round((double) SOLAR_PEAK_KW * clear / kind().parts() * Math.max(0, Math.cos(level.getSunAngle(0))));
    }
    @Override public void onLoad() {
        super.onLoad();
        if (kind() == MachineKind.SOLAR_PANEL) PowerNetwork.register(level, worldPosition, PowerNetwork.Kind.GENERATOR);
    }
    @Override public void setRemoved() {
        if (kind() == MachineKind.SOLAR_PANEL) PowerNetwork.unregister(level, worldPosition, PowerNetwork.Kind.GENERATOR);
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
            case BURNER_MINER -> GeneratorEntity.isFuel(stack);
            case FURNACE -> slot == 1 ? GeneratorEntity.isFuel(stack) : slot == 0 && IndustrialSmelting.find(level, stack) != null;
            case ASSEMBLER -> {
                CraftingRecipe recipe = level == null ? null : selectedRecipe();
                var requirements = recipe == null ? java.util.List.<AssemblyRecipes.Requirement>of() : AssemblyRecipes.requirements(recipe);
                yield slot < requirements.size() && requirements.get(slot).ingredient().test(stack);
            }
            case LAB -> slot == 0 ? stack.is(FactoryContent.RED_SCIENCE_ITEM.get()) : slot == 1 && stack.is(FactoryContent.GREEN_SCIENCE_ITEM.get());
            case SOLAR_PANEL -> false;
        };
    }
    @Override public boolean canTakeItem(Container destination, int slot, ItemStack stack) { return slot == outputSlot(); }
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int count) { ItemStack result = ContainerHelper.removeItem(items, slot, count); setChanged(); return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public boolean stillValid(Player player) { return !isRemoved() && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) < 100; }
    @Override public Component getDisplayName() { return Component.translatable("block.conveyance." + kind().id); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        if (kind() == MachineKind.SOLAR_PANEL) return null;
        return kind() == MachineKind.BURNER_MINER ? new GeneratorMenu(id, inventory, this)
                : kind() == MachineKind.LAB ? new GeneratorMenu(id, inventory, this, true) : new ProcessingMenu(id, inventory, this);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("BurnTicks", burnTicks); tag.putDouble("CoalWork", coalWork); tag.putInt("Cursor", cursor);
        tag.putInt("LabResearch", labResearch);
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
        burnTicks = Math.max(0, Math.min(GeneratorEntity.MAX_FUEL_TICKS, tag.getInt("BurnTicks")));
        coalWork = Math.max(0, tag.getDouble("CoalWork")); cursor = Math.floorMod(tag.getInt("Cursor"), 4);
        labResearch = tag.getInt("LabResearch");
        selectedRecipe = tag.contains("SelectedRecipe") ? ResourceLocation.tryParse(tag.getString("SelectedRecipe")) : null;
        creativeRecipe = tag.getBoolean("CreativeRecipe");
        smeltingRecipe = tag.contains("SmeltingRecipe") ? ResourceLocation.tryParse(tag.getString("SmeltingRecipe")) : null;
    }
}
