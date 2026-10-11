package dev.conveyance.factory;

import dev.conveyance.Conveyance;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Connected ports share storage immediately, without distance or pipe throughput limits. */
@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID)
public final class FluidNetwork {
    private record Node(BlockPos root, int channel) {}
    private record Socket(BlockPos pos, Direction side) {}
    private static final Map<Level, Grid> GRIDS = new WeakHashMap<>();
    private static final class Grid {
        final Set<BlockPos> roots = new HashSet<>();
        final Map<Socket, Node> sockets = new HashMap<>();
        final Map<Node, Network> assigned = new HashMap<>();
        final List<Network> networks = new ArrayList<>();
        boolean dirty = true;
    }
    private static final class Network {
        final List<Node> nodes = new ArrayList<>();
        double amount, capacity;
        int requiredMask, contentMask, engines;
        FluidKind fluid = FluidKind.EMPTY;
        boolean changed;
        boolean mixed() { return Integer.bitCount(requiredMask | contentMask) > 1; }
    }
    static void register(Level level, BlockPos root) {
        if (level == null) return;
        Grid grid = GRIDS.computeIfAbsent(level, ignored -> new Grid());
        grid.roots.add(root.immutable()); grid.dirty = true;
    }
    static void unregister(Level level, BlockPos root) {
        if (level == null) return;
        Grid grid = GRIDS.get(level);
        if (grid == null) return;
        if (!level.isClientSide) flush(level, grid);
        grid.roots.remove(root); grid.dirty = true;
    }
    public static void invalidate(Level level) {
        Grid grid = GRIDS.get(level); if (grid != null) grid.dirty = true;
    }
    public static void settle(Level level) {
        if (!level.isClientSide) flush(level, refresh(level));
    }
    private static FluidEndpoint endpoint(Level level, BlockPos root) {
        if (!level.hasChunkAt(root)) return null;
        BlockEntity entity = level.getBlockEntity(root);
        return entity instanceof FluidEndpoint endpoint && !entity.isRemoved() ? endpoint : null;
    }
    private static FluidTank tank(Level level, Node node) {
        FluidEndpoint endpoint = endpoint(level, node.root); return endpoint == null ? null : endpoint.fluidTank(node.channel);
    }
    private static Grid refresh(Level level) {
        Grid grid = GRIDS.computeIfAbsent(level, ignored -> new Grid());
        if (!grid.dirty) return grid;
        if (!level.isClientSide) flush(level, grid);
        grid.sockets.clear(); grid.assigned.clear(); grid.networks.clear();
        grid.roots.removeIf(root -> endpoint(level, root) == null);
        Map<Node, Node> parents = new HashMap<>();
        for (BlockPos root : grid.roots) {
            FluidEndpoint endpoint = endpoint(level, root);
            for (var port : endpoint.fluidPorts()) {
                Node node = new Node(root, port.channel());
                parents.putIfAbsent(node, node); grid.sockets.put(new Socket(port.pos(), port.side()), node);
            }
        }
        for (var entry : grid.sockets.entrySet()) {
            Socket socket = entry.getKey();
            Node neighbour = grid.sockets.get(new Socket(socket.pos.relative(socket.side), socket.side.getOpposite()));
            if (neighbour != null) parents.put(find(parents, entry.getValue()), find(parents, neighbour));
        }
        Map<Node, Network> components = new HashMap<>();
        for (Node node : parents.keySet()) {
            Network network = components.computeIfAbsent(find(parents, node), ignored -> new Network());
            FluidTank tank = tank(level, node);
            network.nodes.add(node); network.capacity += tank.capacity; network.amount += tank.amount();
            network.requiredMask |= tank.required.mask;
            if (tank.amount() > 0.00000001) network.contentMask |= tank.fluid().mask;
            if (level.isClientSide && level.getBlockState(node.root).getBlock() instanceof PipeBlock)
                network.contentMask |= FluidKind.byId(level.getBlockState(node.root).getValue(PipeBlock.FLUID)).mask;
            if (level.getBlockEntity(node.root) instanceof MachineEntity machine && machine.kind() == MachineKind.STEAM_ENGINE) network.engines++;
            grid.assigned.put(node, network);
        }
        for (Network network : components.values()) {
            int mask = network.requiredMask | network.contentMask;
            network.fluid = FluidKind.byMask(mask);
            network.changed = true;
        }
        grid.networks.addAll(components.values()); grid.dirty = false;
        return grid;
    }
    private static <T> T find(Map<T, T> parents, T key) {
        T root = key;
        while (!parents.get(root).equals(root)) root = parents.get(root);
        while (!key.equals(root)) { T next = parents.get(key); parents.put(key, root); key = next; }
        return root;
    }
    private static Network network(Level level, BlockPos root, int channel) {
        return refresh(level).assigned.get(new Node(root, channel));
    }
    public static double amount(Level level, BlockPos root, int channel) {
        Network network = network(level, root, channel); return network == null || network.mixed() ? 0 : network.amount;
    }
    public static FluidKind fluid(Level level, BlockPos root, int channel) {
        Network network = network(level, root, channel);
        return network == null || network.mixed() ? FluidKind.EMPTY : network.fluid;
    }
    public static double localAmount(Level level, BlockPos root, int channel) {
        Network network = network(level, root, channel); FluidTank tank = tank(level, new Node(root, channel));
        return network == null || network.mixed() || tank == null || network.capacity == 0 ? 0 : network.amount * tank.capacity / network.capacity;
    }
    public static double room(Level level, BlockPos root, int channel, FluidKind fluid) {
        Network network = network(level, root, channel);
        return network == null || network.mixed() || network.fluid != FluidKind.EMPTY && network.fluid != fluid ? 0 : Math.max(0, network.capacity - network.amount);
    }
    public static double fill(Level level, BlockPos root, int channel, FluidKind fluid, double quantity) {
        Network network = network(level, root, channel);
        double accepted = Math.min(Math.max(0, quantity), room(level, root, channel, fluid));
        if (accepted > 0) {
            network.fluid = fluid; network.contentMask = fluid.mask; network.amount += accepted; network.changed = true;
        }
        return accepted;
    }
    public static double drain(Level level, BlockPos root, int channel, FluidKind fluid, double quantity) {
        Network network = network(level, root, channel);
        if (network == null || network.mixed() || network.fluid != fluid) return 0;
        double removed = Math.min(network.amount, Math.max(0, quantity));
        if (removed > 0) {
            network.amount = Math.max(0, network.amount - removed); network.changed = true;
            if (network.amount < 0.00000001) {
                network.amount = 0; network.contentMask = 0;
                network.fluid = FluidKind.byMask(network.requiredMask);
            }
        }
        return removed;
    }
    static double steamPerEngine(Level level, BlockPos root) {
        Network network = network(level, root, 1);
        return network == null || network.mixed() ? 0 : network.amount / Math.max(1, network.engines);
    }
    /** Validate all new channels together, including two ports touching the same empty pipe network. */
    public static boolean canConnect(Level level, List<FluidEndpoint.Port> ports) {
        Grid grid = refresh(level);
        Map<Object, Object> parents = new HashMap<>(); Map<Object, Integer> masks = new HashMap<>();
        for (var port : ports) {
            Object channel = port.tank(); parents.putIfAbsent(channel, channel); masks.put(channel, port.tank().fluid().mask);
            Node neighbour = grid.sockets.get(new Socket(port.pos().relative(port.side()), port.side().getOpposite()));
            Network network = neighbour == null ? null : grid.assigned.get(neighbour);
            if (network == null) continue;
            parents.putIfAbsent(network, network); masks.put(network, network.requiredMask | network.contentMask);
            parents.put(find(parents, channel), find(parents, network));
        }
        Map<Object, Integer> combined = new HashMap<>();
        for (Object key : parents.keySet()) {
            Object root = find(parents, key);
            int mask = combined.getOrDefault(root, 0) | masks.getOrDefault(key, 0);
            if (Integer.bitCount(mask) > 1) return false;
            combined.put(root, mask);
        }
        return true;
    }
    public static boolean hasPort(Level level, BlockPos pos, Direction side) {
        if (!level.hasChunkAt(pos)) return false;
        if (level.getBlockState(pos).getBlock() instanceof PipeBlock) return true;
        var machine = MachineBlock.controller(level, pos);
        FluidEndpoint endpoint = machine != null ? machine : level.getBlockEntity(pos) instanceof FluidEndpoint fluid ? fluid : null;
        return endpoint != null && endpoint.fluidPorts().stream().anyMatch(port -> port.pos().equals(pos) && port.side() == side);
    }
    public static void showStatus(Level level, BlockPos pos, Player player) {
        Network network = network(level, pos, 0);
        if (network == null) return;
        player.displayClientMessage(network.mixed() ? Component.translatable("message.conveyance.fluid_mixing")
                : Component.translatable("message.conveyance.fluid_status", network.fluid.title(),
                    FactoryTooltips.number(network.amount), FactoryTooltips.number(network.capacity)), true);
    }
    private static void flush(Level level, Grid grid) {
        for (Network network : grid.networks) {
            if (!network.changed || network.mixed() || network.capacity <= 0) continue;
            for (Node node : network.nodes) {
                FluidTank tank = tank(level, node); if (tank == null) continue;
                if (tank.store(network.fluid, network.amount * tank.capacity / network.capacity)) level.getBlockEntity(node.root).setChanged();
                if (level.getBlockEntity(node.root) instanceof PipeEntity) {
                    var state = level.getBlockState(node.root);
                    if (state.getBlock() instanceof PipeBlock && state.getValue(PipeBlock.FLUID) != network.fluid.ordinal())
                        level.setBlockAndUpdate(node.root, state.setValue(PipeBlock.FLUID, network.fluid.ordinal()));
                }
            }
            network.changed = false;
        }
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
        Grid grid = GRIDS.get(event.level); if (grid != null) flush(event.level, refresh(event.level));
    }
    private FluidNetwork() {}
}
