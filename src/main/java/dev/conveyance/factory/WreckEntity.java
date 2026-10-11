package dev.conveyance.factory;

import dev.conveyance.player.InventoryAccess;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Only the wreck root owns saved machine state and recoverable contents. */
public final class WreckEntity extends BlockEntity {
    private BlockPos root;
    private BlockState original;
    private List<BuildingDamage.Part> pieces = List.of();
    private CompoundTag machine = new CompoundTag();
    private List<ItemStack> contents = List.of();
    public WreckEntity(BlockPos pos, BlockState state) { super(FactoryContent.WRECK_ENTITY.get(), pos, state); root = pos; }
    public BlockPos root() { return root; }
    public static WreckEntity at(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof WreckEntity part) || !level.hasChunkAt(part.root)) return null;
        return level.getBlockEntity(part.root) instanceof WreckEntity wreck ? wreck : null;
    }
    public void setRoot(BlockPos pos) { root = pos.immutable(); changed(); }
    public void store(BlockState state, List<BuildingDamage.Part> parts, CompoundTag data, List<ItemStack> loot) {
        original = state; pieces = List.copyOf(parts); machine = data.copy(); contents = List.copyOf(loot); changed();
    }
    private void changed() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
    public boolean restore(ServerPlayer player) {
        if (original == null || pieces.isEmpty()) return false;
        for (var part : pieces) {
            if (!level.hasChunkAt(part.pos())) return false;
            var state = level.getBlockState(part.pos());
            if (!state.isAir() && !(level.getBlockEntity(part.pos()) instanceof WreckEntity wreck && wreck.root.equals(root))) return false;
            if (!level.isUnobstructed(part.state(), part.pos(), net.minecraft.world.phys.shapes.CollisionContext.empty())) return false;
        }
        var saved = machine.copy(); var parts = pieces; var origin = root;
        for (var part : parts) level.setBlockAndUpdate(part.pos(), part.state());
        BlockEntity restored = level.getBlockEntity(origin);
        if (restored != null) { restored.load(saved); restored.setChanged(); level.sendBlockUpdated(origin, restored.getBlockState(), restored.getBlockState(), 3); }
        BuildingDamage.clear(player.serverLevel(), origin); FluidNetwork.invalidate(level);
        return true;
    }
    public boolean salvage(ServerPlayer player) {
        var extra = InventoryAccess.extra(player); var inventory = InventoryAccess.snapshot(player, extra);
        for (ItemStack item : contents) if (!InventoryAccess.insert(inventory, item)) return false;
        var parts = pieces;
        for (var part : parts) if (level.hasChunkAt(part.pos()) && level.getBlockEntity(part.pos()) instanceof WreckEntity wreck && wreck.root.equals(root)) level.removeBlock(part.pos(), false);
        InventoryAccess.commit(player, extra, inventory); return true;
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.putLong("Root", root.asLong());
        if (original == null) return;
        tag.put("Original", NbtUtils.writeBlockState(original)); tag.put("Machine", machine);
        var parts = new ListTag();
        for (var part : pieces) { var entry = new CompoundTag(); entry.putLong("Pos", part.pos().asLong()); entry.put("State", NbtUtils.writeBlockState(part.state())); parts.add(entry); }
        tag.put("Pieces", parts); var loot = new ListTag(); for (ItemStack item : contents) loot.add(item.save(new CompoundTag())); tag.put("Contents", loot);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); root = tag.contains("Root") ? BlockPos.of(tag.getLong("Root")) : worldPosition;
        original = tag.contains("Original") ? NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Original")) : null;
        machine = tag.getCompound("Machine").copy(); var parts = new ArrayList<BuildingDamage.Part>(); var saved = tag.getList("Pieces", 10);
        for (int i = 0; i < saved.size(); i++) { var entry = saved.getCompound(i); parts.add(new BuildingDamage.Part(BlockPos.of(entry.getLong("Pos")), NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), entry.getCompound("State")))); }
        pieces = List.copyOf(parts); var loot = new ArrayList<ItemStack>(); saved = tag.getList("Contents", 10);
        for (int i = 0; i < saved.size(); i++) { var item = ItemStack.of(saved.getCompound(i)); if (!item.isEmpty()) loot.add(item); } contents = List.copyOf(loot);
    }
    @Override public CompoundTag getUpdateTag() { var tag = new CompoundTag(); tag.putLong("Root", root.asLong()); return tag; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
