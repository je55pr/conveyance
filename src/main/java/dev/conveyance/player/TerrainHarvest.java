package dev.conveyance.player;

import dev.conveyance.world.TerrainBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class TerrainHarvest {
    public static boolean terrain(BlockState state) {
        return TerrainBlocks.natural(state);
    }
    public static ItemStack resource(BlockState state) {
        state = TerrainBlocks.material(state);
        if (state.is(BlockTags.IRON_ORES)) return new ItemStack(Items.RAW_IRON);
        if (state.is(BlockTags.COPPER_ORES)) return new ItemStack(Items.RAW_COPPER);
        if (state.is(BlockTags.COAL_ORES)) return new ItemStack(Items.COAL);
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.COBBLESTONE)) return new ItemStack(Items.COBBLESTONE);
        if (state.is(BlockTags.DIRT) || state.is(Blocks.GRASS_BLOCK)) return new ItemStack(Items.DIRT);
        if (state.is(Blocks.SAND)) return new ItemStack(Items.SAND);
        if (state.is(Blocks.RED_SAND)) return new ItemStack(Items.RED_SAND);
        if (state.is(Blocks.GRAVEL)) return new ItemStack(Items.GRAVEL);
        if (state.is(Blocks.CLAY)) return new ItemStack(Items.CLAY_BALL);
        return ItemStack.EMPTY;
    }
    @SubscribeEvent public static void leftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (terrain(event.getLevel().getBlockState(event.getPos()))) event.setCanceled(true);
    }
    @SubscribeEvent public static void breakBlock(BlockEvent.BreakEvent event) {
        if (terrain(event.getState())) event.setCanceled(true);
    }
    public static void harvest(ServerPlayer player, BlockPos pos) {
        if (!player.isAlive() || player.isSpectator() || !player.getAbilities().mayBuild
                || !player.level().hasChunkAt(pos) || !player.level().mayInteract(player, pos)
                || !dev.conveyance.factory.RemoteBuilding.canReach(player, pos) && pos.distToCenterSqr(player.getEyePosition()) > 36) return;
        long now = player.level().getGameTime();
        long previous = player.getPersistentData().getLong("ConveyanceHarvestTime");
        if (now - previous < Math.ceil(18 / ResearchState.handHarvestSpeed(player.level()))) return;
        BlockState state = player.level().getBlockState(pos);
        if (!terrain(state)) return;
        player.getPersistentData().putLong("ConveyanceHarvestTime", now);
        ItemStack output = resource(state);
        if (output.isEmpty()) player.displayClientMessage(Component.translatable("message.conveyance.terrain_protected"), true);
        else if (!InventoryAccess.give(player, output)) player.displayClientMessage(Component.translatable("message.conveyance.inventory_full"), true);
    }
    private TerrainHarvest() {}
}
