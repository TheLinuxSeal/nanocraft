package org.sutormin.nanocraft.data.definitions;

import org.sutormin.nanocraft.data.registry.Registry;
import org.sutormin.nanocraft.data.types.BlockShape;

public class BlockShapeDefinitions {

    private static final float[][] UV = {
            {0.0f, 0.0f},
            {0.0f, 1.0f},
            {1.0f, 1.0f},
            {1.0f, 0.0f}
    };

    public static void define(Registry<BlockShape> reg) {
        defineFull(reg);
        defineCross(reg);
        defineSlab(reg);
        defineStairs(reg);
        defineFence(reg);
        definePane(reg);
    }

    private static void defineFull(Registry<BlockShape> reg) {
        BlockShape shape = reg.addNew("full");

        int v000 = shape.addVertex(0.0f, 0.0f, 0.0f);
        int v001 = shape.addVertex(0.0f, 0.0f, 1.0f);
        int v010 = shape.addVertex(0.0f, 1.0f, 0.0f);
        int v011 = shape.addVertex(0.0f, 1.0f, 1.0f);

        int v100 = shape.addVertex(1.0f, 0.0f, 0.0f);
        int v101 = shape.addVertex(1.0f, 0.0f, 1.0f);
        int v110 = shape.addVertex(1.0f, 1.0f, 0.0f);
        int v111 = shape.addVertex(1.0f, 1.0f, 1.0f);

        shape.addFace(
                new int[]{v010, v011, v111, v110},
                BlockShape.Direction.UP,
                true,
                UV
        );

        shape.addFace(
                new int[]{v000, v100, v101, v001},
                BlockShape.Direction.DOWN,
                true,
                UV
        );

        shape.addFace(
                new int[]{v001, v101, v111, v011},
                BlockShape.Direction.SOUTH,
                true,
                UV
        );

        shape.addFace(
                new int[]{v000, v010, v110, v100},
                BlockShape.Direction.NORTH,
                true,
                UV
        );

        shape.addFace(
                new int[]{v100, v110, v111, v101},
                BlockShape.Direction.EAST,
                true,
                UV
        );

        shape.addFace(
                new int[]{v000, v001, v011, v010},
                BlockShape.Direction.WEST,
                true,
                UV
        );
    }

    private static void defineCross(Registry<BlockShape> reg) {
        BlockShape shape = reg.addNew("cross");

        // Plane running X/Z diagonally.
        int a = shape.addVertex(0.0f, 0.0f, 0.0f);
        int b = shape.addVertex(1.0f, 0.0f, 1.0f);
        int c = shape.addVertex(1.0f, 1.0f, 1.0f);
        int d = shape.addVertex(0.0f, 1.0f, 0.0f);

        shape.addFace(
                new int[]{a, b, c, d},
                BlockShape.Direction.NORTH,
                false,
                UV
        );

        shape.addFace(
                new int[]{d, c, b, a},
                BlockShape.Direction.SOUTH,
                false,
                UV
        );

        // Other diagonal plane.
        int e = shape.addVertex(1.0f, 0.0f, 0.0f);
        int f = shape.addVertex(0.0f, 0.0f, 1.0f);
        int g = shape.addVertex(0.0f, 1.0f, 1.0f);
        int h = shape.addVertex(1.0f, 1.0f, 0.0f);

        shape.addFace(
                new int[]{e, f, g, h},
                BlockShape.Direction.EAST,
                false,
                UV
        );

        shape.addFace(
                new int[]{h, g, f, e},
                BlockShape.Direction.WEST,
                false,
                UV
        );
    }

    private static void defineSlab(Registry<BlockShape> reg) {
        BlockShape shape = reg.addNew("slab");

        int v000 = shape.addVertex(0.0f, 0.0f, 0.0f);
        int v001 = shape.addVertex(0.0f, 0.0f, 1.0f);
        int v100 = shape.addVertex(1.0f, 0.0f, 0.0f);
        int v101 = shape.addVertex(1.0f, 0.0f, 1.0f);

        int v010 = shape.addVertex(0.0f, 0.5f, 0.0f);
        int v011 = shape.addVertex(0.0f, 0.5f, 1.0f);
        int v110 = shape.addVertex(1.0f, 0.5f, 0.0f);
        int v111 = shape.addVertex(1.0f, 0.5f, 1.0f);

        shape.addFace(
                new int[]{v010, v011, v111, v110},
                BlockShape.Direction.UP,
                true,
                UV
        );

        shape.addFace(
                new int[]{v000, v100, v101, v001},
                BlockShape.Direction.DOWN,
                true,
                UV
        );

        shape.addFace(
                new int[]{v001, v101, v111, v011},
                BlockShape.Direction.SOUTH,
                true,
                UV
        );

        shape.addFace(
                new int[]{v000, v010, v110, v100},
                BlockShape.Direction.NORTH,
                true,
                UV
        );

        shape.addFace(
                new int[]{v100, v110, v111, v101},
                BlockShape.Direction.EAST,
                true,
                UV
        );

        shape.addFace(
                new int[]{v000, v001, v011, v010},
                BlockShape.Direction.WEST,
                true,
                UV
        );
    }

    private static void defineStairs(Registry<BlockShape> reg) {
        BlockShape shape = reg.addNew("stairs");

        // Lower half.
        int v000 = shape.addVertex(0.0f, 0.0f, 0.0f);
        int v001 = shape.addVertex(0.0f, 0.0f, 1.0f);
        int v100 = shape.addVertex(1.0f, 0.0f, 0.0f);
        int v101 = shape.addVertex(1.0f, 0.0f, 1.0f);

        int v010 = shape.addVertex(0.0f, 0.5f, 0.0f);
        int v011 = shape.addVertex(0.0f, 0.5f, 1.0f);
        int v110 = shape.addVertex(1.0f, 0.5f, 0.0f);
        int v111 = shape.addVertex(1.0f, 0.5f, 1.0f);

        // Upper step.
        int v020 = shape.addVertex(0.0f, 1.0f, 0.0f);
        int v021 = shape.addVertex(0.0f, 1.0f, 0.5f);
        int v120 = shape.addVertex(1.0f, 1.0f, 0.0f);
        int v121 = shape.addVertex(1.0f, 1.0f, 0.5f);

        // Top of upper step.
        shape.addFace(
                new int[]{v020, v021, v121, v120},
                BlockShape.Direction.UP,
                true,
                UV
        );

        // Top of lower step.
        shape.addFace(
                new int[]{v010, v011, v111, v110},
                BlockShape.Direction.UP,
                true,
                UV
        );

        // Bottom.
        shape.addFace(
                new int[]{v000, v100, v101, v001},
                BlockShape.Direction.DOWN,
                true,
                UV
        );

        // South.
        shape.addFace(
                new int[]{v001, v101, v111, v011},
                BlockShape.Direction.SOUTH,
                true,
                UV
        );

        // North.
        shape.addFace(
                new int[]{v000, v010, v110, v100},
                BlockShape.Direction.NORTH,
                true,
                UV
        );

        // East side.
        shape.addFace(
                new int[]{v100, v110, v120, v121, v111, v101},
                BlockShape.Direction.EAST,
                true,
                new float[][]{
                        {0.0f, 0.0f},
                        {0.0f, 0.5f},
                        {0.0f, 1.0f},
                        {1.0f, 1.0f},
                        {1.0f, 0.5f},
                        {1.0f, 0.0f}
                }
        );

        // West side.
        shape.addFace(
                new int[]{v000, v001, v011, v021, v020, v010},
                BlockShape.Direction.WEST,
                true,
                new float[][]{
                        {0.0f, 0.0f},
                        {1.0f, 0.0f},
                        {1.0f, 0.5f},
                        {1.0f, 1.0f},
                        {0.0f, 1.0f},
                        {0.0f, 0.5f}
                }
        );
    }

    private static void defineFence(Registry<BlockShape> reg) {
        BlockShape shape = reg.addNew("fence");

        float x0 = 0.4375f;
        float x1 = 0.5625f;
        float z0 = 0.4375f;
        float z1 = 0.5625f;

        int a = shape.addVertex(x0, 0.0f, z0);
        int b = shape.addVertex(x1, 0.0f, z0);
        int c = shape.addVertex(x1, 1.0f, z0);
        int d = shape.addVertex(x0, 1.0f, z0);

        int e = shape.addVertex(x0, 0.0f, z1);
        int f = shape.addVertex(x1, 0.0f, z1);
        int g = shape.addVertex(x1, 1.0f, z1);
        int h = shape.addVertex(x0, 1.0f, z1);

        shape.addFace(
                new int[]{d, c, g, h},
                BlockShape.Direction.UP,
                true,
                UV
        );

        shape.addFace(
                new int[]{a, e, f, b},
                BlockShape.Direction.DOWN,
                true,
                UV
        );

        shape.addFace(
                new int[]{a, b, c, d},
                BlockShape.Direction.NORTH,
                true,
                UV
        );

        shape.addFace(
                new int[]{f, e, h, g},
                BlockShape.Direction.SOUTH,
                true,
                UV
        );

        shape.addFace(
                new int[]{b, f, g, c},
                BlockShape.Direction.EAST,
                true,
                UV
        );

        shape.addFace(
                new int[]{e, a, d, h},
                BlockShape.Direction.WEST,
                true,
                UV
        );
    }

    private static void definePane(Registry<BlockShape> reg) {
        BlockShape shape = reg.addNew("pane");

        float min = 0.46875f;
        float max = 0.53125f;

        int a = shape.addVertex(min, 0.0f, min);
        int b = shape.addVertex(max, 0.0f, min);
        int c = shape.addVertex(max, 1.0f, min);
        int d = shape.addVertex(min, 1.0f, min);

        int e = shape.addVertex(min, 0.0f, max);
        int f = shape.addVertex(max, 0.0f, max);
        int g = shape.addVertex(max, 1.0f, max);
        int h = shape.addVertex(min, 1.0f, max);

        shape.addFace(
                new int[]{a, b, c, d},
                BlockShape.Direction.NORTH,
                false,
                UV
        );

        shape.addFace(
                new int[]{h, g, f, e},
                BlockShape.Direction.SOUTH,
                false,
                UV
        );

        shape.addFace(
                new int[]{b, f, g, c},
                BlockShape.Direction.EAST,
                false,
                UV
        );

        shape.addFace(
                new int[]{e, h, d, a},
                BlockShape.Direction.WEST,
                false,
                UV
        );
    }
}