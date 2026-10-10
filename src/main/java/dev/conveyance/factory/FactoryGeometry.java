package dev.conveyance.factory;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Collision and selection bounds exported from the same source as the meshes. */
public final class FactoryGeometry {
    private static final Map<String, VoxelShape[][]> SHAPES = load();
    public static VoxelShape shape(String name, int part, Direction facing) {
        var parts = SHAPES.get(name);
        if (parts == null || part < 0 || part >= parts.length) return Shapes.empty();
        int turn = switch (facing) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
        return parts[part][turn];
    }
    private static Map<String, VoxelShape[][]> load() {
        var shapes = new HashMap<String, VoxelShape[][]>();
        try (var stream = FactoryGeometry.class.getResourceAsStream("/assets/conveyance/machine_shapes.json")) {
            if (stream == null) throw new IllegalStateException("Missing Conveyance machine geometry");
            var data = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var machine : data.entrySet()) {
                var parts = machine.getValue().getAsJsonArray();
                var rotated = new VoxelShape[parts.size()][4];
                for (int part = 0; part < parts.size(); part++) for (int turn = 0; turn < 4; turn++) {
                    VoxelShape shape = Shapes.empty();
                    for (var entry : parts.get(part).getAsJsonArray()) {
                        var box = entry.getAsJsonArray();
                        double x0 = box.get(0).getAsDouble(), y0 = box.get(1).getAsDouble(), z0 = box.get(2).getAsDouble();
                        double x1 = box.get(3).getAsDouble(), y1 = box.get(4).getAsDouble(), z1 = box.get(5).getAsDouble();
                        for (int i = 0; i < turn; i++) {
                            double oldX0 = x0, oldX1 = x1;
                            x0 = 1 - z1; x1 = 1 - z0; z0 = oldX0; z1 = oldX1;
                        }
                        shape = Shapes.joinUnoptimized(shape, Shapes.box(x0, y0, z0, x1, y1, z1), BooleanOp.OR);
                    }
                    rotated[part][turn] = shape.optimize();
                }
                shapes.put(machine.getKey(), rotated);
            }
        } catch (java.io.IOException failure) { throw new IllegalStateException("Could not load Conveyance machine geometry", failure); }
        return shapes;
    }
    private FactoryGeometry() {}
}
