package org.sutormin.nanocraft.world;

import org.sutormin.nanocraft.data.types.BlockShape;
import org.sutormin.nanocraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Face geometry helpers and the face-pair cull cache used by {@link Chunk}
 * during meshing.
 *
 * <p>Whether one face is fully hidden by another is a pure function of
 * their geometry (vertices + direction) -- it never depends on where in the
 * world the two blocks actually sit. So instead of recomputing footprints
 * for every single block instance in the world, the result is computed
 * once per unique (face, face) pair and cached forever. Faces are shared,
 * immutable objects owned by their registered {@link BlockShape}, so
 * identity is stable and safe to key on. Both caches are static (shared
 * across every {@link Chunk}) and backed by {@link ConcurrentHashMap} since
 * mesh building often happens on worker threads; {@code computeIfAbsent}
 * guarantees each pair/face is computed exactly once even under concurrent
 * access.
 */
public final class FaceCullCache {
    private FaceCullCache() {}

    // ------------------------------------------------------------------
    // Per-direction face basis
    //
    // Each of the 6 axis-aligned directions is described by a unit normal
    // (n) and two unit tangent axes (u, v). This is the only thing
    // hardcoded per-direction; it's used both for culling (which neighbor
    // to test) and, by Chunk, for AO sampling and vertex placement.
    // ------------------------------------------------------------------

    public record FaceBasis(int nx, int ny, int nz, int ux, int uy, int uz, int vx, int vy, int vz) {}

    public static FaceBasis basisOf(BlockShape.Direction dir) {
        return switch (dir) {
            case UP    -> new FaceBasis(0, 1, 0,  0, 0, 1,  1, 0, 0);
            case DOWN  -> new FaceBasis(0, -1, 0, 1, 0, 0,  0, 0, 1);
            case NORTH -> new FaceBasis(0, 0, 1,  1, 0, 0,  0, 1, 0); // +Z
            case SOUTH -> new FaceBasis(0, 0, -1, 0, 1, 0,  1, 0, 0); // -Z
            case WEST  -> new FaceBasis(-1, 0, 0, 0, 0, 1,  0, 1, 0); // -X
            case EAST  -> new FaceBasis(1, 0, 0,  0, 1, 0,  0, 0, 1); // +X
        };
    }

    static BlockShape.Direction opposite(BlockShape.Direction dir) {
        return switch (dir) {
            case UP -> BlockShape.Direction.DOWN;
            case DOWN -> BlockShape.Direction.UP;
            case NORTH -> BlockShape.Direction.SOUTH;
            case SOUTH -> BlockShape.Direction.NORTH;
            case EAST -> BlockShape.Direction.WEST;
            case WEST -> BlockShape.Direction.EAST;
        };
    }

    // ------------------------------------------------------------------
    // Footprints
    // ------------------------------------------------------------------

    /**
     * The 2D footprint a face leaves on its normal axis' boundary plane:
     * where along the normal axis it sits ({@code normalCoord}, 0..128),
     * and its extent along the two tangent axes. Two faces on opposite
     * sides of a block boundary can only touch each other if they both
     * report {@link #touchesOwnBoundary()}; one fully hides the other only
     * if it {@link #covers(Footprint)} it.
     */
    private record Footprint(boolean normalPositive, int normalCoord,
                             int minA, int maxA, int minB, int maxB) {
        boolean touchesOwnBoundary() {
            return normalPositive ? normalCoord >= 128 : normalCoord <= 0;
        }

        boolean covers(Footprint other) {
            return minA <= other.minA && maxA >= other.maxA
                    && minB <= other.minB && maxB >= other.maxB;
        }
    }

    private static Footprint computeFootprint(List<BlockShape.Vertex> verts, BlockShape.Face face, FaceBasis basis) {
        int[] idx = face.vertices();
        int minA = Integer.MAX_VALUE, maxA = Integer.MIN_VALUE;
        int minB = Integer.MAX_VALUE, maxB = Integer.MIN_VALUE;
        int normalCoord = 0;

        for (int i : idx) {
            BlockShape.Vertex v = verts.get(i);
            int a, b;
            // Project onto whichever two axes aren't the face's normal axis.
            if (basis.nx() != 0) { normalCoord = v.x(); a = v.y(); b = v.z(); }
            else if (basis.ny() != 0) { normalCoord = v.y(); a = v.x(); b = v.z(); }
            else { normalCoord = v.z(); a = v.x(); b = v.y(); }

            if (a < minA) minA = a;
            if (a > maxA) maxA = a;
            if (b < minB) minB = b;
            if (b > maxB) maxB = b;
        }

        boolean positive = basis.nx() > 0 || basis.ny() > 0 || basis.nz() > 0;
        return new Footprint(positive, normalCoord, minA, maxA, minB, maxB);
    }

    // ------------------------------------------------------------------
    // Caches
    // ------------------------------------------------------------------

    private record FacePair(BlockShape.Face a, BlockShape.Face b) {}

    private static final Map<FacePair, Boolean> CULL_CACHE = new ConcurrentHashMap<>();

    // Whether a face touches its own block's boundary is also a pure,
    // neighbor-independent fact -- cache it separately so a face that could
    // never be culled (e.g. anything sitting mid-block) short-circuits
    // before Chunk even looks up the neighbor.
    private static final Map<BlockShape.Face, Boolean> BOUNDARY_CACHE = new ConcurrentHashMap<>();

    public static boolean touchesOwnBoundary(BlockShape.Face face, List<BlockShape.Vertex> verts) {
        return BOUNDARY_CACHE.computeIfAbsent(face,
                f -> computeFootprint(verts, f, basisOf(f.dir())).touchesOwnBoundary());
    }

    private static boolean cullsAgainst(BlockShape.Face a, List<BlockShape.Vertex> vertsA,
                                        BlockShape.Face b, List<BlockShape.Vertex> vertsB) {
        return CULL_CACHE.computeIfAbsent(new FacePair(a, b), k -> computeCulls(a, vertsA, b, vertsB));
    }

    private static boolean computeCulls(BlockShape.Face a, List<BlockShape.Vertex> vertsA,
                                        BlockShape.Face b, List<BlockShape.Vertex> vertsB) {
        if (b.dir() != opposite(a.dir())) return false;
        if (!touchesOwnBoundary(a, vertsA)) return false;
        if (!touchesOwnBoundary(b, vertsB)) return false;

        Footprint fa = computeFootprint(vertsA, a, basisOf(a.dir()));
        Footprint fb = computeFootprint(vertsB, b, basisOf(b.dir()));
        return fb.covers(fa);
    }

    /**
     * Checks whether {@code face} (with vertices {@code verts}) is hidden
     * by any of {@code neighborShape}'s faces on the opposite side. This is
     * the main entry point {@link Chunk} calls once it has already
     * confirmed {@code face} touches its own boundary and located a
     * non-null neighbor shape.
     */
    public static boolean occludes(BlockShape.Face face, List<BlockShape.Vertex> verts, BlockShape neighborShape) {
        BlockShape.Direction opposite = opposite(face.dir());
        List<BlockShape.Vertex> neighborVerts = neighborShape.getVertices();

        for (BlockShape.Face candidate : neighborShape.getFaces()) {
            if (candidate.dir() != opposite) continue;
            if (cullsAgainst(face, verts, candidate, neighborVerts)) return true;
        }

        return false;
    }

    /**
     * Eagerly fills the cull cache for every (face, opposite-direction face)
     * pair across the given shapes. Purely a warm-up -- the cache fills
     * itself in correctly on demand without this -- but calling it once
     * right after all BlockShapes are registered (world/game load) means
     * the very first meshes built don't pay for any cache misses. Assumes
     * shape geometry doesn't change after registration; call
     * {@link #clear()} first if it does.
     */
    public static void preload(Collection<BlockShape> shapes) {
        record Entry(BlockShape.Face face, List<BlockShape.Vertex> verts) {}

        List<Entry> all = new ArrayList<>();
        for (BlockShape shape : shapes) {
            for (BlockShape.Face f : shape.getFaces()) {
                all.add(new Entry(f, shape.getVertices()));
            }
        }

        for (Entry a : all) {
            for (Entry b : all) {
                if (b.face().dir() == opposite(a.face().dir())) {
                    cullsAgainst(a.face(), a.verts(), b.face(), b.verts());
                }
            }
        }
    }

    public static void clear() {
        CULL_CACHE.clear();
        BOUNDARY_CACHE.clear();
    }
}