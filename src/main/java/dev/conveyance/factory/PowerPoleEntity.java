package dev.conveyance.factory;

import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class PowerPoleEntity extends BlockEntity {
    private List<BlockPos> connections = List.of();
    public PowerPoleEntity(BlockPos pos, BlockState state) { super(FactoryContent.POWER_POLE_ENTITY.get(), pos, state); }

    public static void tick(Level level, BlockPos pos, BlockState state, PowerPoleEntity pole) {
        if (level.getGameTime() % 20 == 0) PowerNetwork.update(level);
    }
    @Override public void onLoad() {
        super.onLoad();
        PowerNetwork.register(level, worldPosition, PowerNetwork.Kind.POLE);
    }
    @Override public void setRemoved() {
        PowerNetwork.unregister(level, worldPosition, PowerNetwork.Kind.POLE);
        super.setRemoved();
    }
    void setConnections(List<BlockPos> links) {
        if (connections.equals(links)) return;
        connections = List.copyOf(links);
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    public List<BlockPos> connections() { return connections; }
    @Override public CompoundTag getUpdateTag() {
        CompoundTag tag = saveWithoutMetadata();
        tag.putLongArray("Connections", connections.stream().mapToLong(BlockPos::asLong).toArray());
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        connections = Arrays.stream(tag.getLongArray("Connections")).mapToObj(BlockPos::of).toList();
    }
    @Override public AABB getRenderBoundingBox() {
        AABB bounds = new AABB(worldPosition).expandTowards(0, PowerPoleBlock.HEIGHT, 0);
        for (BlockPos pos : connections) bounds = bounds.minmax(new AABB(pos).expandTowards(0, PowerPoleBlock.HEIGHT, 0));
        return bounds.inflate(1);
    }
}
