package dev.conveyance.factory;

import dev.conveyance.player.InventoryAccess;
import dev.conveyance.player.LinkedHotbar;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;

public final class FactoryActions {
    public static final int COPY = 0, PASTE = 1, FUEL = 2, COLLECT = 3;
    public static final String CLIPBOARD_KEY = "ConveyanceCopiedRecipe";
    private static Container container(Level level, BlockPos pos) {
        MachineEntity machine = MachineBlock.controller(level, pos);
        if (machine != null) return machine;
        return level.getBlockEntity(pos) instanceof Container container ? container : null;
    }
    public static BlockPos root(Level level, BlockPos pos) {
        MachineEntity machine = MachineBlock.controller(level, pos);
        return machine == null ? pos : machine.getBlockPos();
    }
    public static boolean assembler(Level level, BlockPos pos) {
        MachineEntity machine = MachineBlock.controller(level, pos);
        return machine != null && machine.kind() == MachineKind.ASSEMBLER;
    }
    private static int fuelSlot(Container container) {
        if (container instanceof GeneratorEntity || container instanceof BurnerInserterEntity) return 0;
        if (container instanceof MachineEntity machine) return machine.kind() == MachineKind.FURNACE ? 1 : machine.kind() == MachineKind.BURNER_MINER ? 0 : -1;
        return -1;
    }
    public static boolean fuelTarget(Level level, BlockPos pos, ItemStack held) { return GeneratorEntity.isFuel(held) && fuelSlot(container(level, pos)) >= 0; }
    public static boolean collectionTarget(Level level, BlockPos pos) {
        Container container = container(level, pos);
        return container instanceof CrateEntity || container instanceof MachineEntity machine && machine.outputSlot() >= 0;
    }
    public static void perform(ServerPlayer player, BlockPos pos, int action) {
        Level level = player.level();
        double reach = player.getAttributeValue(ForgeMod.BLOCK_REACH.get()) + 1;
        if (!player.isAlive() || player.isSpectator() || !player.getAbilities().mayBuild || !level.hasChunkAt(pos)
                || pos.distToCenterSqr(player.getEyePosition()) > reach * reach || player.containerMenu != player.inventoryMenu) return;
        MachineEntity machine = MachineBlock.controller(level, pos);
        if (action == COPY) {
            if (machine == null || machine.kind() != MachineKind.ASSEMBLER || machine.selectedRecipe() == null) return;
            var recipe = machine.selectedRecipe();
            player.getPersistentData().putString(CLIPBOARD_KEY, recipe.getId().toString());
            player.displayClientMessage(Component.translatable("message.conveyance.recipe_copied", recipe.getResultItem(level.registryAccess()).getHoverName()), true);
        } else if (action == PASTE) {
            ResourceLocation id = ResourceLocation.tryParse(player.getPersistentData().getString(CLIPBOARD_KEY));
            if (id != null && machine != null && machine.selectRecipe(player, id))
                player.displayClientMessage(Component.translatable("message.conveyance.recipe_pasted", machine.selectedRecipe().getResultItem(level.registryAccess()).getHoverName()), true);
        } else if (action == FUEL) fillFuel(player, container(level, pos));
        else if (action == COLLECT) collect(player, container(level, pos));
    }
    private static void fillFuel(ServerPlayer player, Container container) {
        LinkedHotbar.refreshHand(player);
        ItemStack held = player.getMainHandItem();
        int slot = fuelSlot(container);
        if (slot < 0 || !GeneratorEntity.isFuel(held)) return;
        ItemStack stored = container.getItem(slot);
        if (!stored.isEmpty() && !ItemStack.isSameItemSameTags(stored, held)) return;
        int room = Math.min(container.getMaxStackSize(), held.getMaxStackSize()) - stored.getCount();
        if (room <= 0) return;
        var extra = InventoryAccess.extra(player);
        var inventory = InventoryAccess.snapshot(player, extra);
        int moved = player.isCreative() ? room : 0;
        if (!player.isCreative()) for (ItemStack source : inventory) {
            if (!ItemStack.isSameItemSameTags(source, held)) continue;
            int taken = Math.min(room - moved, source.getCount());
            source.shrink(taken); moved += taken;
            if (moved == room) break;
        }
        if (moved == 0) return;
        ItemStack fuel = held.copyWithCount(stored.getCount() + moved);
        InventoryAccess.commit(player, extra, inventory);
        container.setItem(slot, fuel); container.setChanged();
        player.displayClientMessage(Component.translatable("message.conveyance.fuel_loaded", moved, fuel.getHoverName()), true);
    }
    private static void collect(ServerPlayer player, Container container) {
        if (!(container instanceof CrateEntity) && !(container instanceof MachineEntity machine && machine.outputSlot() >= 0)) return;
        var extra = InventoryAccess.extra(player);
        var inventory = InventoryAccess.snapshot(player, extra);
        int moved = 0;
        var changes = new java.util.HashMap<Integer, ItemStack>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container instanceof MachineEntity machine && slot != machine.outputSlot()) continue;
            ItemStack original = container.getItem(slot), remaining = InventoryAccess.remainder(inventory, original);
            int taken = original.getCount() - remaining.getCount();
            if (taken > 0) { changes.put(slot, remaining); moved += taken; }
        }
        if (moved == 0) return;
        InventoryAccess.commit(player, extra, inventory);
        changes.forEach(container::setItem); container.setChanged();
        player.displayClientMessage(Component.translatable("message.conveyance.products_collected", moved), true);
    }
    private FactoryActions() {}
}
