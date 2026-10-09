package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class CrateBlock extends BaseEntityBlock {
    public final int slots;
    public CrateBlock(Properties properties, int slots) { super(properties); this.slots = slots; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CrateEntity(pos, state); }
    @Override public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.conveyance.crate", slots, slots * 100));
        super.appendHoverText(stack, level, tooltip, flag);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CrateEntity crate) player.openMenu(crate);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) recover(level, pos, state);
        super.playerWillDestroy(level, pos, state, player);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide) recover(level, pos, state);
        super.onRemove(state, level, pos, next, moving);
    }
    private static void recover(Level level, BlockPos pos, BlockState state) {
        if (!(level.getBlockEntity(pos) instanceof CrateEntity crate) || crate.recovered) return;
        crate.recovered = true;
        ItemStack packed = new ItemStack(state.getBlock());
        if (!crate.isEmpty()) BlockItem.setBlockEntityData(packed, FactoryContent.CRATE_ENTITY.get(), crate.saveWithoutMetadata());
        Block.popResource(level, pos, packed);
    }
}
