package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class GeneratorBlock extends BaseEntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public GeneratorBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(LIT, false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(LIT); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GeneratorEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, FactoryContent.GENERATOR_ENTITY.get(), GeneratorEntity::tick);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof BlockItem) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GeneratorEntity generator) {
            PowerNetwork.showStatus(level, pos, player);
            player.openMenu(generator);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) recover(level, pos);
        super.playerWillDestroy(level, pos, state, player);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide) recover(level, pos);
        super.onRemove(state, level, pos, next, moving);
    }
    private static void recover(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof GeneratorEntity generator) || generator.recovered) return;
        generator.recovered = true;
        ItemStack packed = new ItemStack(FactoryContent.GENERATOR_ITEM.get());
        BlockItem.setBlockEntityData(packed, FactoryContent.GENERATOR_ENTITY.get(), generator.saveWithoutMetadata());
        Block.popResource(level, pos, packed);
    }
}
