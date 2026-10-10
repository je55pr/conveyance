package dev.conveyance.factory;

import dev.conveyance.Conveyance;
import dev.conveyance.ItemPolicy;
import dev.conveyance.player.LinkedHotbar;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Remote access is a session, never a permanent change to vanilla reach. */
@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID)
public final class RemoteBuilding {
    public static final int PLACE = 0, USE = 1, START_BREAK = 2, FINISH_BREAK = 3, CANCEL_BREAK = 4, ROTATE = 5;
    // Client and integrated-server entities share numeric IDs (and therefore equals/hashCode).
    private static final Map<Player, Session> CLIENT_SESSIONS = new WeakHashMap<>(), SERVER_SESSIONS = new WeakHashMap<>();
    private static Map<Player, Session> sessions(Player player) { return player.level().isClientSide ? CLIENT_SESSIONS : SERVER_SESSIONS; }
    private static final class Session {
        final Level level;
        BlockPos breaking;
        long start, lastPlacement = Long.MIN_VALUE;
        Session(Level level) { this.level = level; }
    }
    public static void toggle(Player player, boolean enabled) {
        if (enabled && player.isAlive() && !player.isSpectator()) sessions(player).put(player, new Session(player.level()));
        else sessions(player).remove(player);
    }
    public static boolean active(Player player) {
        Session session = sessions(player).get(player);
        return session != null && session.level == player.level() && player.isAlive() && !player.isSpectator();
    }
    public static boolean inArea(Player player, BlockPos pos) {
        return Math.abs((pos.getX() >> 4) - player.chunkPosition().x) <= 2
                && Math.abs((pos.getZ() >> 4) - player.chunkPosition().z) <= 2
                && !player.level().isOutsideBuildHeight(pos);
    }
    public static boolean canReach(Player player, BlockPos pos) {
        return active(player) && inArea(player, pos) && player.level().hasChunkAt(pos);
    }
    public static PlacementPreview.Plan preview(Player player, BlockPos pos, Direction facing) {
        var plan = PlacementPreview.resolve(new RemotePlacementContext(player, pos, facing));
        if (plan == null) return null;
        boolean valid = plan.valid() && plan.parts().stream().allMatch(part -> inArea(player, part.pos())
                && player.level().hasChunkAt(part.pos()) && player.level().mayInteract(player, part.pos()));
        return new PlacementPreview.Plan(plan.parts(), valid);
    }
    public static void act(ServerPlayer player, int action, BlockPos pos, Direction facing, int slot, boolean secondary) {
        if (!canReach(player, pos) || !player.getAbilities().mayBuild || !player.level().mayInteract(player, pos)
                || player.containerMenu != player.inventoryMenu || slot < 0 || slot >= LinkedHotbar.SIZE || facing.getAxis().isVertical()) return;
        if (slot != LinkedHotbar.selectedSlot(player)) LinkedHotbar.select(player, slot);
        LinkedHotbar.refreshHand(player);
        var session = sessions(player).get(player); var level = player.serverLevel();
        if (action == PLACE) {
            long now = level.getGameTime();
            if (session.lastPlacement == now || ItemPolicy.isDisabled(player.getMainHandItem())) return;
            var plan = preview(player, pos, facing);
            if (plan == null || !plan.valid() || !(player.getMainHandItem().getItem() instanceof BlockItem item)) return;
            session.lastPlacement = now;
            item.place(new RemotePlacementContext(player, pos, facing));
            LinkedHotbar.refreshHand(player); player.inventoryMenu.broadcastChanges();
        } else if (action == START_BREAK) {
            session.breaking = pos.immutable(); session.start = level.getGameTime();
        } else if (action == CANCEL_BREAK) session.breaking = null;
        else if (action == FINISH_BREAK) {
            if (!pos.equals(session.breaking) || level.getGameTime() - session.start < 10) return;
            session.breaking = null;
            var state = level.getBlockState(pos);
            if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0)
                MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player));
        } else if (action == ROTATE) {
            var state = level.getBlockState(pos);
            // Whole multiblocks keep their existing footprint. Single-block logistics can turn in place.
            if ((state.getBlock() instanceof ConveyorBlock || state.getBlock() instanceof InserterBlock)
                    && state.hasProperty(ConveyorBlock.FACING))
                level.setBlockAndUpdate(pos, state.setValue(ConveyorBlock.FACING, state.getValue(ConveyorBlock.FACING).getClockWise()));
        } else if (action == USE) {
            BlockPos root = FactoryActions.root(level, pos);
            var entity = level.getBlockEntity(root);
            if (entity instanceof MenuProvider menu && !(entity instanceof MachineEntity machine && machine.kind() == MachineKind.SOLAR_PANEL)) {
                player.openMenu(menu); return;
            }
            boolean oldShift = player.isShiftKeyDown();
            try {
                player.setShiftKeyDown(secondary);
                var state = level.getBlockState(pos);
                state.use(level, player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos), facing.getOpposite(), pos, false));
                if (entity == null || entity instanceof PoweredMachineEntity) PowerNetwork.showStatus(level, root, player);
            } finally { player.setShiftKeyDown(oldShift); }
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { sessions(event.getEntity()).remove(event.getEntity()); }
    private RemoteBuilding() {}
}
