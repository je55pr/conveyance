package dev.conveyance.world;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Nine flush ground tiles describe one well; tile 9 preserves older single-block wells. */
public final class OilDepositBlock extends Block {
    public static final IntegerProperty TILE = IntegerProperty.create("tile", 0, 9);
    public OilDepositBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(TILE, 9)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(TILE); }
}
