package dev.conveyance.factory;

import dev.conveyance.ItemPolicy;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class MachineEntity extends PoweredMachineEntity implements Container, MenuProvider {
    private NonNullList<ItemStack> items;
    private ResourceLocation selectedRecipe, smeltingRecipe;
    private int burnTicks, coalWork, cookingTime = 200, cursor;

    public MachineEntity(BlockPos pos, BlockState state) {
        super(FactoryContent.MACHINE_ENTITY.get(), pos, state, MachineBlock.kind(state) == MachineKind.ASSEMBLER ? 60 : 0, 40);
        items = NonNullList.withSize(kind().slots, ItemStack.EMPTY);
    }
    public MachineKind kind() { return MachineBlock.kind(getBlockState()); }
    public int outputSlot() { return kind() == MachineKind.BURNER_MINER ? -1 : kind() == MachineKind.FURNACE ? 2 : 4; }
    public int fuelRemaining() { return burnTicks; }
    public int progressPixels() { return (int) (24 * (kind() == MachineKind.ASSEMBLER ? currentWork() / 40 : (double) coalWork / cookingTime)); }

    @Override public double powerDistanceSquared(BlockPos pole) {
        BlockPos far = MachineBlock.partPosition(worldPosition, getBlockState().getValue(MachineBlock.FACING), kind(), kind().parts() - 1);
        int x = Math.max(Math.min(worldPosition.getX(), far.getX()), Math.min(Math.max(worldPosition.getX(), far.getX()), pole.getX()));
        int y = Math.max(worldPosition.getY(), Math.min(far.getY(), pole.getY()));
        int z = Math.max(Math.min(worldPosition.getZ(), far.getZ()), Math.min(Math.max(worldPosition.getZ(), far.getZ()), pole.getZ()));
        return pole.distSqr(new BlockPos(x, y, z));
    }
    public static void tick(Level level, BlockPos pos, BlockState state, MachineEntity machine) {
        switch (machine.kind()) {
            case BURNER_MINER -> machine.mine();
            case FURNACE -> machine.smelt();
            case ASSEMBLER -> machine.assemble();
        }
        boolean lit = machine.kind() != MachineKind.ASSEMBLER && machine.burnTicks > 0;
        if (state.getValue(MachineBlock.LIT) != lit) level.setBlockAndUpdate(pos, state.setValue(MachineBlock.LIT, lit));
    }
    private boolean burn(int slot) {
        if (burnTicks == 0) {
            if (!GeneratorEntity.isFuel(items.get(slot))) return false;
            items.get(slot).shrink(1);
            burnTicks = GeneratorEntity.FUEL_TICKS;
            setChanged();
        }
        burnTicks--;
        if (level.getGameTime() % 20 == 0) setChanged();
        return true;
    }
    private void mine() {
        var facing = getBlockState().getValue(MachineBlock.FACING);
        ConveyorEntity belt = ConveyorEntity.at(level, worldPosition.relative(facing));
        if (belt == null || !belt.canAccept(facing.getOpposite())) return;
        ItemStack output = ItemStack.EMPTY;
        int nextCursor = cursor;
        for (int attempt = 0; attempt < 4; attempt++) {
            int part = (cursor + attempt) % 4;
            var ore = level.getBlockState(MachineBlock.partPosition(worldPosition, facing, kind(), part).below());
            output = ore.is(Blocks.IRON_ORE) ? new ItemStack(Items.RAW_IRON) : ore.is(Blocks.COPPER_ORE) ? new ItemStack(Items.RAW_COPPER)
                    : ore.is(Blocks.COAL_ORE) ? new ItemStack(Items.COAL) : ItemStack.EMPTY;
            if (!output.isEmpty()) { nextCursor = (part + 1) % 4; break; }
        }
        if (output.isEmpty() || !burn(0)) return;
        if (++coalWork < 40) return;
        belt.offer(output, facing.getOpposite());
        coalWork = 0;
        cursor = nextCursor;
        setChanged();
    }
    private void smelt() {
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(items.get(0)), level).orElse(null);
        if (recipe == null) { coalWork = 0; return; }
        if (!recipe.getId().equals(smeltingRecipe)) { smeltingRecipe = recipe.getId(); coalWork = 0; setChanged(); }
        ItemStack result = recipe.assemble(new SimpleContainer(items.get(0)), level.registryAccess());
        if (!fitsOutput(result) || !burn(1)) return;
        cookingTime = Math.max(1, recipe.getCookingTime());
        if (++coalWork < cookingTime) return;
        items.get(0).shrink(1);
        putOutput(result);
        coalWork = 0;
        setChanged();
    }
    public CraftingRecipe selectedRecipe() {
        if (selectedRecipe == null) {
            var recipes = AssemblyRecipes.available(level);
            if (recipes.isEmpty()) return null;
            selectedRecipe = recipes.stream().filter(recipe -> recipe.getId().getPath().equals("conveyor")).findFirst().orElse(recipes.get(0)).getId();
            setChanged();
        }
        var recipe = level.getRecipeManager().byKey(selectedRecipe).orElse(null);
        return recipe instanceof CraftingRecipe crafting ? crafting : null;
    }
    public int selectedRecipeIndex() {
        CraftingRecipe selected = selectedRecipe();
        var recipes = AssemblyRecipes.available(level);
        for (int index = 0; index < recipes.size(); index++) if (selected != null && recipes.get(index).getId().equals(selected.getId())) return index;
        return -1;
    }
    public void changeRecipe(int direction) {
        var recipes = AssemblyRecipes.available(level);
        if (recipes.isEmpty()) return;
        selectedRecipe = recipes.get(Math.floorMod(selectedRecipeIndex() + direction, recipes.size())).getId();
        resetWork();
    }
    private void assemble() {
        CraftingRecipe recipe = selectedRecipe();
        if (recipe == null) return;
        var requirements = AssemblyRecipes.requirements(recipe);
        if (requirements.size() > 4 || requirements.isEmpty()) return;
        for (int slot = 0; slot < requirements.size(); slot++) {
            var requirement = requirements.get(slot);
            if (!requirement.ingredient().test(items.get(slot)) || items.get(slot).getCount() < requirement.count()) return;
        }
        ItemStack result = recipe.getResultItem(level.registryAccess());
        if (!fitsOutput(result)) return;
        if (!advanceWork()) return;
        for (int slot = 0; slot < requirements.size(); slot++) items.get(slot).shrink(requirements.get(slot).count());
        putOutput(result);
        setChanged();
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
            case FURNACE -> slot == 1 ? GeneratorEntity.isFuel(stack) : slot == 0 && level != null
                    && level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(stack), level).isPresent();
            case ASSEMBLER -> {
                CraftingRecipe recipe = level == null ? null : selectedRecipe();
                var requirements = recipe == null ? java.util.List.<AssemblyRecipes.Requirement>of() : AssemblyRecipes.requirements(recipe);
                yield slot < requirements.size() && requirements.get(slot).ingredient().test(stack);
            }
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
        return kind() == MachineKind.BURNER_MINER ? new GeneratorMenu(id, inventory, this) : new ProcessingMenu(id, inventory, this);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("BurnTicks", burnTicks); tag.putInt("CoalWork", coalWork); tag.putInt("Cursor", cursor);
        if (smeltingRecipe != null) tag.putString("SmeltingRecipe", smeltingRecipe.toString());
        if (selectedRecipe != null) {
            tag.putString("SelectedRecipe", selectedRecipe.toString());
        }
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(kind().slots, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        burnTicks = Math.max(0, Math.min(GeneratorEntity.FUEL_TICKS, tag.getInt("BurnTicks")));
        coalWork = Math.max(0, tag.getInt("CoalWork")); cursor = Math.floorMod(tag.getInt("Cursor"), 4);
        selectedRecipe = tag.contains("SelectedRecipe") ? ResourceLocation.tryParse(tag.getString("SelectedRecipe")) : null;
        smeltingRecipe = tag.contains("SmeltingRecipe") ? ResourceLocation.tryParse(tag.getString("SmeltingRecipe")) : null;
    }
}
