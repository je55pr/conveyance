package dev.conveyance.factory;

import dev.conveyance.world.TerrainBlocks;

import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

final class MiningResources {
    record Pick(ItemStack stack, int nextCursor) {}
    static Pick find(Level level, int area, int cursor, IntFunction<BlockPos> position) {
        // Preserve ore-focused drills when stone also touches their footprint.
        for (int pass = 0; pass < 2; pass++) {
            for (int attempt = 0; attempt < area; attempt++) {
                int part = Math.floorMod(cursor + attempt, area);
                BlockPos pos = position.apply(part);
                if (!level.hasChunkAt(pos)) continue;
                BlockState state = TerrainBlocks.material(level.getBlockState(pos));
                ItemStack output = pass == 0 ? ore(state) : stone(state);
                if (!output.isEmpty()) return new Pick(output, (part + 1) % area);
            }
        }
        return new Pick(ItemStack.EMPTY, cursor);
    }
    private static ItemStack ore(BlockState state) {
        return state.is(BlockTags.IRON_ORES) ? new ItemStack(Items.RAW_IRON)
                : state.is(BlockTags.COPPER_ORES) ? new ItemStack(Items.RAW_COPPER)
                : state.is(BlockTags.COAL_ORES) ? new ItemStack(Items.COAL) : ItemStack.EMPTY;
    }
    private static ItemStack stone(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.BLACKSTONE)
                ? new ItemStack(Items.COBBLESTONE) : ItemStack.EMPTY;
    }
    private MiningResources() {}
}
