package dev.conveyance.combat;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.conveyance.Conveyance;
import dev.conveyance.factory.BuildingDamage;
import dev.conveyance.factory.CombatRepair;
import dev.conveyance.factory.FactoryContent;
import dev.conveyance.player.InventoryAccess;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID)
public final class CombatEvents {
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void repair(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().is(FactoryContent.REPAIR_PACK.get())) return;
        event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS);
        if (!event.getLevel().isClientSide) CombatRepair.repair(event.getEntity(), event.getPos());
    }
    @SubscribeEvent public static void attack(AttackEntityEvent event) {
        if (event.getEntity().getMainHandItem().is(FactoryContent.PISTOL.get())) event.setCanceled(true);
    }
    @SubscribeEvent public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && BuildingDamage.building(event.getPlacedBlock())) BuildingDamage.clear(level, event.getPos());
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("conveyance").requires(source -> source.hasPermission(2))
                .then(Commands.literal("combat")
                        .then(Commands.literal("kit").executes(context -> {
                            var player = context.getSource().getPlayerOrException();
                            for (var stack : new ItemStack[]{new ItemStack(FactoryContent.PISTOL.get()), new ItemStack(FactoryContent.FIREARM_MAGAZINE.get(), 100),
                                    new ItemStack(FactoryContent.REPAIR_PACK.get(), 20), new ItemStack(FactoryContent.GUN_TURRET_ITEM.get(), 8), new ItemStack(FactoryContent.STONE_WALL_ITEM.get(), 100)}) InventoryAccess.returnItem(player, stack);
                            context.getSource().sendSuccess(() -> Component.literal("Combat supplies added to your inventory."), false); return 1;
                        }))
                        .then(Commands.literal("wave").executes(context -> spawn(context.getSource().getLevel(), BlockPos.containing(context.getSource().getPosition()), 8))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64)).executes(context -> spawn(context.getSource().getLevel(), BlockPos.containing(context.getSource().getPosition()), IntegerArgumentType.getInteger(context, "count")))))
                        .then(Commands.literal("nest").executes(context -> {
                            var source = context.getSource(); var player = source.getPlayerOrException();
                            var direction = player.getLookAngle().multiply(1, 0, 1).normalize();
                            var near = BlockPos.containing(player.position().add(direction.scale(48)));
                            var pos = EnemyNests.get(source.getLevel()).debugNest(source.getLevel(), near);
                            source.sendSuccess(() -> pos == null ? Component.translatable("message.conveyance.nest_no_space") : Component.translatable("message.conveyance.nest_created", pos.getX(), pos.getZ()), false);
                            return pos == null ? 0 : 1;
                        }))
                        .then(Commands.literal("provoke").executes(context -> {
                            var source = context.getSource(); var nests = EnemyNests.get(source.getLevel()); var pos = nests.nearest(BlockPos.containing(source.getPosition()), 256);
                            if (pos == null || !nests.provoke(source.getLevel(), pos, BlockPos.containing(source.getPosition()))) { source.sendFailure(Component.translatable("message.conveyance.no_nest")); return 0; }
                            source.sendSuccess(() -> Component.translatable("message.conveyance.nest_provoked", pos.getX(), pos.getZ()), false); return 1;
                        }))
                        .then(Commands.literal("nest_info").executes(context -> {
                            var source = context.getSource(); var nests = EnemyNests.get(source.getLevel()); var pos = nests.nearest(BlockPos.containing(source.getPosition()), 256);
                            source.sendSuccess(() -> pos == null ? Component.translatable("message.conveyance.no_nest") : nests.info(source.getLevel(), pos), false); return pos == null ? 0 : 1;
                        }))
                        .then(Commands.literal("damage").then(Commands.argument("amount", IntegerArgumentType.integer(1, 100000)).executes(context -> {
                            var player = context.getSource().getPlayerOrException();
                            var hit = player.pick(32, 0, false);
                            return hit instanceof net.minecraft.world.phys.BlockHitResult block && BuildingDamage.hurt(player.serverLevel(), block.getBlockPos(), IntegerArgumentType.getInteger(context, "amount")) ? 1 : 0;
                        })))
                        .then(Commands.literal("clear").executes(context -> {
                            var source = context.getSource(); int count = 0;
                            for (var entity : source.getLevel().getEntitiesOfClass(BiterEntity.class, new net.minecraft.world.phys.AABB(source.getPosition(), source.getPosition()).inflate(128))) { entity.discard(); count++; }
                            source.sendSuccess(() -> Component.literal("Nearby biters removed."), false); return count;
                        }))));
    }
    private static int spawn(ServerLevel level, BlockPos center, int count) {
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            double angle = i * Math.PI * 2 / count;
            int x = center.getX() + (int) Math.round(Math.cos(angle) * 26), z = center.getZ() + (int) Math.round(Math.sin(angle) * 26);
            if (!level.hasChunk(x >> 4, z >> 4)) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            var pos = new BlockPos(x, y, z);
            if (!level.getFluidState(pos.below()).isEmpty()) continue;
            var enemy = CombatContent.BITER.get().create(level); if (enemy == null) continue;
            enemy.moveTo(x + .5, y, z + .5, 0, 0);
            if (!level.noCollision(enemy)) continue;
            enemy.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.COMMAND, null, null);
            enemy.attackFactory(center); if (level.addFreshEntity(enemy)) spawned++;
        }
        return spawned;
    }
    private CombatEvents() {}
}
