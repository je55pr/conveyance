package dev.conveyance.factory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Shared generation and demand, with no capacity limit on a pole connection. */
public final class PowerNetwork {
    public static final int LINK_RANGE = 24, COVERAGE_RANGE = 8;
    enum Kind { POLE, GENERATOR, MACHINE }
    private static final Map<Level, Grid> GRIDS = new WeakHashMap<>();

    // Grids contain positions, never Level or BlockEntity references, so closed worlds can be collected.
    private static final class Grid {
        final Map<BlockPos, Kind> nodes = new HashMap<>();
        final Map<BlockPos, Network> assigned = new HashMap<>();
        final List<Network> networks = new ArrayList<>();
        boolean dirty = true;
        long statsTime = Long.MIN_VALUE;
    }
    private static final class Network {
        final List<BlockPos> poles = new ArrayList<>(), generators = new ArrayList<>(), machines = new ArrayList<>();
        int supply, demand;
        double fraction() { return demand == 0 ? 1 : Math.min(1, (double) supply / demand); }
    }
    private record Edge(int first, int second, double distance) {}

    static void register(Level level, BlockPos pos, Kind kind) {
        if (level == null || level.isClientSide) return;
        Grid grid = GRIDS.computeIfAbsent(level, ignored -> new Grid());
        grid.nodes.put(pos.immutable(), kind);
        grid.dirty = true;
    }
    static void unregister(Level level, BlockPos pos, Kind kind) {
        if (level == null || level.isClientSide) return;
        Grid grid = GRIDS.get(level);
        if (grid != null && grid.nodes.remove(pos, kind)) grid.dirty = true;
    }
    static void invalidateSupply(Level level) {
        Grid grid = GRIDS.get(level);
        if (grid != null) grid.statsTime = Long.MIN_VALUE;
    }

    public static double fraction(Level level, BlockPos pos) {
        Network network = refresh(level).assigned.get(pos);
        return network == null ? 0 : network.fraction();
    }

    public static void showStatus(Level level, BlockPos pos, Player player) {
        Network network = refresh(level).assigned.get(pos);
        Component message = network == null ? Component.translatable("message.conveyance.no_power_coverage")
                : network.demand == 0 ? Component.translatable("message.conveyance.power_idle", network.supply, network.poles.size())
                : Component.translatable("message.conveyance.power_status", network.supply, network.demand,
                        Math.round(network.fraction() * 100), network.poles.size(), network.machines.size());
        player.displayClientMessage(message, true);
    }

    static void update(Level level) { refresh(level); }
    private static Grid refresh(Level level) {
        Grid grid = GRIDS.computeIfAbsent(level, ignored -> new Grid());
        if (grid.dirty) rebuild(level, grid);
        long now = level.getGameTime();
        if (grid.statsTime != Long.MIN_VALUE && now - grid.statsTime < 20) return grid;
        grid.statsTime = now;
        for (Network network : grid.networks) {
            network.supply = network.demand = 0;
            for (BlockPos pos : network.generators) {
                if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof PowerProducer generator) {
                    network.supply += generator.outputPower();
                }
            }
            for (BlockPos pos : network.machines) {
                if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof PoweredMachineEntity machine) {
                    network.demand += machine.powerDemand();
                }
            }
        }
        return grid;
    }

    private static boolean validNode(Level level, BlockPos pos, Kind kind) {
        if (!level.hasChunkAt(pos)) return false;
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null || entity.isRemoved()) return false;
        return switch (kind) {
            case POLE -> entity instanceof PowerPoleEntity;
            case GENERATOR -> entity instanceof PowerProducer;
            case MACHINE -> entity instanceof PoweredMachineEntity;
        };
    }

    private static void rebuild(Level level, Grid grid) {
        grid.nodes.entrySet().removeIf(entry -> !validNode(level, entry.getKey(), entry.getValue()));
        grid.assigned.clear();
        grid.networks.clear();
        List<BlockPos> poles = grid.nodes.entrySet().stream().filter(entry -> entry.getValue() == Kind.POLE)
                .map(Map.Entry::getKey).sorted(Comparator.comparingLong(BlockPos::asLong)).toList();
        int[] parents = new int[poles.size()];
        List<List<BlockPos>> links = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();
        for (int first = 0; first < poles.size(); first++) {
            parents[first] = first;
            links.add(new ArrayList<>());
            for (int second = first + 1; second < poles.size(); second++) {
                double distance = poles.get(first).distSqr(poles.get(second));
                if (distance <= LINK_RANGE * LINK_RANGE) edges.add(new Edge(first, second, distance));
            }
        }
        // Nearest connections form a spanning forest instead of drawing every possible wire.
        edges.sort(Comparator.comparingDouble(Edge::distance).thenComparingInt(Edge::first).thenComparingInt(Edge::second));
        for (Edge edge : edges) {
            int firstRoot = root(parents, edge.first), secondRoot = root(parents, edge.second);
            if (firstRoot == secondRoot) continue;
            parents[firstRoot] = secondRoot;
            links.get(edge.first).add(poles.get(edge.second));
            links.get(edge.second).add(poles.get(edge.first));
        }
        Map<Integer, Network> components = new HashMap<>();
        for (int index = 0; index < poles.size(); index++) {
            Network network = components.computeIfAbsent(root(parents, index), ignored -> new Network());
            BlockPos pos = poles.get(index);
            network.poles.add(pos);
            grid.assigned.put(pos, network);
            if (level.getBlockEntity(pos) instanceof PowerPoleEntity pole) pole.setConnections(links.get(index));
        }
        grid.networks.addAll(components.values());
        for (var entry : grid.nodes.entrySet()) {
            if (entry.getValue() == Kind.POLE) continue;
            BlockPos pos = entry.getKey(), closest = null;
            BlockEntity entity = level.getBlockEntity(pos);
            double bestDistance = COVERAGE_RANGE * COVERAGE_RANGE;
            for (BlockPos pole : poles) {
                double distance = entity instanceof PoweredMachineEntity machine
                        ? machine.powerDistanceSquared(pole) : pos.distSqr(pole);
                if (distance <= bestDistance && (closest == null || distance < bestDistance)) {
                    closest = pole;
                    bestDistance = distance;
                }
            }
            if (closest != null) {
                Network network = grid.assigned.get(closest);
                grid.assigned.put(pos, network);
                if (entry.getValue() == Kind.GENERATOR) network.generators.add(pos);
                else network.machines.add(pos);
            }
        }
        grid.dirty = false;
        grid.statsTime = Long.MIN_VALUE;
    }

    private static int root(int[] parents, int node) {
        while (parents[node] != node) {
            parents[node] = parents[parents[node]];
            node = parents[node];
        }
        return node;
    }
    private PowerNetwork() {}
}
