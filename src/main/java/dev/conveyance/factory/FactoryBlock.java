package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;

public abstract class FactoryBlock extends BaseEntityBlock {
    protected FactoryBlock(Properties properties) { super(properties); }
    @Override public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) { return 0.1F; }
}
