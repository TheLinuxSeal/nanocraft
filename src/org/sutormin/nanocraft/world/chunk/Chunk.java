package org.sutormin.nanocraft.world.chunk;

import org.sutormin.nanocraft.NanoCraft;
import org.sutormin.nanocraft.data.Registries;
import org.sutormin.nanocraft.data.quickaccess.QuickAccessBlocks;
import org.sutormin.nanocraft.data.types.Block;
import org.sutormin.nanocraft.data.types.BlockShape;
import org.sutormin.nanocraft.world.FaceCullCache;
import org.sutormin.nanocraft.world.render.Mesh;

import java.util.List;

public class Chunk {
    public static final int ATLAS_SIZE = 8;
    public static final float TILE_SIZE = 1.0f / ATLAS_SIZE;
    public static final int SIZE_X = 16;
    public static final int SIZE_Y = 384;
    public static final int SIZE_Z = 16;

    public static final int SEA_LEVEL = 63;

    private final ChunkPos worldPos;
    // 32 bits for: 16b = blockid, 16b = blockstate (redstone level, orientation, etc)
    private char[] blocks = new char[SIZE_X * SIZE_Y * SIZE_Z];

    private char[] vArray = new char[1024];
    private int vCount = 0;
    private int[] iArray = new int[1024];
    private int iCount = 0;

    public Mesh mesh;

    public Chunk(ChunkPos worldPos) {
        this.worldPos = worldPos;
        this.mesh = new Mesh();
        mesh.setPos(this.worldPos.x(), this.worldPos.z());
    }

    public void setBlocks(char[] blocks) {
        this.blocks = blocks;
    }

    // ------------------------------------------------------------------
    // Noise (unrelated to meshing, kept as-is)
    // ------------------------------------------------------------------

    private int hash(int x, int z) {
        int h = x * 374761393 ^ z * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return Math.abs(h ^ (h >>> 16));
    }

    private int hash(int x, int y, int z) {
        int h = x * 374761393 ^ y * 668265263 ^ z * 83492791;
        h = (h ^ (h >>> 13)) * 1274126177;
        return (h ^ (h >>> 16)) & 0x7FFFFFFF;
    }

    private float sampleNoise2D(float x, float z) {
        int xi = (int) Math.floor(x);
        int zi = (int) Math.floor(z);

        float xf = x - (float) Math.floor(x);
        float zf = z - (float) Math.floor(z);

        float u = xf * xf * (3 - 2 * xf);
        float v = zf * zf * (3 - 2 * zf);

        int g00 = hash(xi, zi) % 4;
        int g10 = hash(xi + 1, zi) % 4;
        int g01 = hash(xi, zi + 1) % 4;
        int g11 = hash(xi + 1, zi + 1) % 4;

        float n00 = grad(g00, xf, zf);
        float n10 = grad(g10, xf - 1, zf);
        float n01 = grad(g01, xf, zf - 1);
        float n11 = grad(g11, xf - 1, zf - 1);

        float x1 = n00 + u * (n10 - n00);
        float x2 = n01 + u * (n11 - n01);

        return x1 + v * (x2 - x1);
    }

    private float grad(int hash, float x, float z) {
        return switch (hash & 3) {
            case 0 -> x + z;
            case 1 -> -x + z;
            case 2 -> x - z;
            default -> -x - z;
        };
    }

    // ------------------------------------------------------------------
    // Face geometry model
    //
    // A block's visible geometry now comes from its BlockShape: an explicit
    // list of vertices (in the same 0..128 per-axis fixed-point space the
    // mesh already packs into) plus a list of faces, each a set of vertex
    // indices, a BlockShape.Direction, and a shouldCull flag. This replaces
    // the previous hardcoded "6 faces of a unit cube" model, so a block can
    // define any shape (slabs, stairs, custom models, cross-plants, ...)
    // while culling and AO both stay correct.
    //
    // The per-direction (normal, tangent u, tangent v) basis, and the
    // face-vs-face cull caching, live in FaceCullCache; Chunk just uses
    // them for meshing.
    // ------------------------------------------------------------------

    /**
     * Looks up the shape of the block at chunk-local (x,y,z), which may
     * reach into a neighboring chunk (mirrors {@link #isTransparent}'s
     * bounds handling). Returns {@code null} for air, out-of-world-height
     * space, or an unloaded neighbor chunk -- anywhere there's nothing to
     * occlude against.
     */
    private BlockShape getShapeAt(int x, int y, int z) {
        char id;
        if (y < 0 || y >= SIZE_Y) return null;
        if (x < 0 || x >= SIZE_X || z < 0 || z >= SIZE_X) {
            id = getBlockInterchunk(x, y, z);
            if (id == QuickAccessBlocks.NULL) return null; // unloaded chunk -> can't occlude
        } else {
            id = blocks[getIndex(x, y, z)];
        }
        if (id == QuickAccessBlocks.AIR) return null;

        Block neighborType = Registries.BLOCK.get(id);
        return neighborType != null ? neighborType.getShape() : null;
    }

    /**
     * Checks whether {@code face} is actually hidden by whatever's on the
     * other side of it. See {@link FaceCullCache} for the actual geometry
     * test and caching; this just wires it up with this chunk's neighbor
     * lookup.
     */
    private boolean isFaceOccluded(int x, int y, int z, BlockShape.Face face, List<BlockShape.Vertex> verts) {
        if (!FaceCullCache.touchesOwnBoundary(face, verts)) return false; // cached; skips the neighbor lookup entirely

        FaceCullCache.FaceBasis basis = FaceCullCache.basisOf(face.dir());
        BlockShape neighborShape = getShapeAt(x + basis.nx(), y + basis.ny(), z + basis.nz());
        if (neighborShape == null) return false;

        return FaceCullCache.occludes(face, verts, neighborShape);
    }

    // ------------------------------------------------------------------
    // Meshing
    // ------------------------------------------------------------------

    public void buildMesh() {
        vArray = new char[1024];
        vCount = 0;
        iArray = new int[1024];
        iCount = 0;
        //System.out.println("buildMesh for " + worldPos + ", blocks[0]=" + (int) blocks[22852]);

        int worldOffsetX = worldPos.x() * SIZE_X;
        int worldOffsetZ = worldPos.z() * SIZE_Z;

        for (int x = 0; x < SIZE_X; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                for (int z = 0; z < SIZE_Z; z++) {
                    char blockId = blocks[getIndex(x, y, z)];
                    if (blockId == QuickAccessBlocks.AIR) continue;

                    Block block = Registries.BLOCK.get(blockId);
                    BlockShape shape = block.getShape();
                    if (shape == null) continue;

                    int wx = worldOffsetX + x;
                    int wz = worldOffsetZ + z;
                    List<BlockShape.Vertex> verts = shape.getVertices();
                    List<BlockShape.Face> faces = shape.getFaces();
                    for (int i = 0; i < faces.size(); i++) {
                        BlockShape.Face face = faces.get(i);
                        if (i >= block.getTextureCount()) {  // or however you can check array length safely
                            System.err.println("Block " + block.getName() + " (id=" + (int) block.getId() + ") has "
                                    + faces.size() + " faces but only " + block.getTextureCount() + " textures");
                        }
                        FaceCullCache.FaceBasis basis = FaceCullCache.basisOf(face.dir());

                        // shouldCull=true means "attempt culling": actually
                        // check what's on the other side (air, an unloaded
                        // chunk, or a neighbor block whose own geometry may
                        // or may not fully cover this face) rather than
                        // blindly hiding it. shouldCull=false skips the
                        // check entirely and always renders the face (e.g.
                        // a cross-plant quad that should never be culled).
                        if (face.shouldCull() && isFaceOccluded(x, y, z, face, verts)) {
                            continue;
                        }

                        addFace(wx, y, wz, x, y, z, basis, verts, face, block.getTexture(i));
                    }
                }
            }
        }

        mesh.updateMesh(vArray, vCount, iArray, iCount);
    }

    /**
     * Emits one BlockShape face (an arbitrary vertex fan, not necessarily a
     * quad) for the block at world position (wx,wy,wz) / chunk-local
     * position (lx,ly,lz).
     */
    private void addFace(int wx, int wy, int wz, int lx, int ly, int lz,
                         FaceCullCache.FaceBasis basis, List<BlockShape.Vertex> verts,
                         BlockShape.Face face, int tex) {
        int[] indices = face.vertices();
        int n = indices.length;
        if (n < 3) return; // not a renderable polygon

        int startIndex = vCount / 7;
        float[] aos = new float[n];

        for (int i = 0; i < n; i++) {
            BlockShape.Vertex v = verts.get(indices[i]);
            aos[i] = vertexAO(lx, ly, lz, basis, v);

            char gx = (char) (((wx & 15) << 7) + v.x());
            char gy = (char) ((wy << 7) + v.y());
            char gz = (char) (((wz & 15) << 7) + v.z());

            int uv1 = Math.round(face.uv()[i][0] * 128.0f);
            int uv2 = Math.round(face.uv()[i][1] * 128.0f);
            char uv = (char) (uv1 * 129 + uv2);

            char ao = (char) Math.floor(aos[i]*65535);

            char texH = (char) (tex >>> 16);
            char texL = (char) (tex & 0xFFFF);

            pushVertex(gx);
            pushVertex(gy);
            pushVertex(gz);
            pushVertex(uv);
            pushVertex(ao);
            pushVertex(texL);
            pushVertex(texH);

            /*long packed =
                    ((long) (gx & 0xFFFL) << 0)
                            | ((long) (gy & 0xFFFFL) << 12)
                            | ((long) (gz & 0xFFFL) << 28)
                            | ((long) (i & 0x3L) << 40)   // UV corner, cycles 0..3 (quad-style atlas mapping)
                            | ((long) (tex & 0xFFFFL) << 42)
                            | ((long) (aos[i] & 0x3L) << 58);

            pushVertex(packed);*/
        }

        if (n == 4) {
            // Standard quad: flip the diagonal split based on AO so lighting
            // interpolates smoothly instead of producing a visible seam
            // ("anisotropy fix"). Assumes vertices are listed BL, BR, TR, TL.
            float ao0 = aos[0];
            float ao1 = aos[1];
            float ao2 = aos[2];
            float ao3 = aos[3];

            if (ao0 + ao2 < ao1 + ao3) {
                pushIndex(startIndex);     pushIndex(startIndex + 1); pushIndex(startIndex + 3);
                pushIndex(startIndex + 1); pushIndex(startIndex + 2); pushIndex(startIndex + 3);
            } else {
                pushIndex(startIndex);     pushIndex(startIndex + 1); pushIndex(startIndex + 2);
                pushIndex(startIndex + 2); pushIndex(startIndex + 3); pushIndex(startIndex);
            }
        } else {
            // Arbitrary polygon: simple fan triangulation from vertex 0.
            for (int i = 1; i < n - 1; i++) {
                pushIndex(startIndex);
                pushIndex(startIndex + i);
                pushIndex(startIndex + i + 1);
            }
        }
    }

    private void pushVertex(char v) {
        if (vCount == vArray.length)
            vArray = java.util.Arrays.copyOf(vArray, vArray.length * 2);
        vArray[vCount++] = v;
    }

    private void pushIndex(int i) {
        if (iCount == iArray.length)
            iArray = java.util.Arrays.copyOf(iArray, iArray.length * 2);
        iArray[iCount++] = i;
    }

    // ------------------------------------------------------------------
    // Ambient occlusion
    // ------------------------------------------------------------------

    /*private float getAOValue(byte index) {
        return switch (index) {
            case 0b10 -> 0.8f;
            case 0b01 -> 0.6f;
            case 0b00 -> 0.4f;
            default -> 0.9f; // 0 blocks: completely open air (bright)
        };
    }*/

    private float getAOIndex(boolean side1, boolean side2, boolean corner) {
        if (side1 && side2) return 0.4f; // 3 blocks: corner enclosed (darkest)
        int count = 0;
        if (side1) count++;
        if (side2) count++;
        if (corner) count++;

        return switch (count) {
            case 1 -> 0.8f;
            case 2 -> 0.6f;
            case 3 -> 0.4f;
            default -> 1.0f; // 0 blocks: completely open air (bright)
        };
    }

    /**
     * Computes AO for one vertex of a face on the block at chunk-local
     * (x,y,z). Generalized from the original's six hand-written per-face
     * switch cases into one routine driven by the face's normal/tangent
     * basis and the vertex's own position, so it works for any face
     * direction *and* any vertex position on that face -- not just the
     * four corners of a full unit cube. A vertex sitting past the block's
     * midpoint (64 in the 0..128 fixed-point range) along a tangent axis
     * is treated as being on that axis's positive side for AO sampling
     * purposes, and vice versa; a vertex sitting exactly on the midpoint
     * doesn't get an extra diagonal/edge sample in that axis.
     */
    private float vertexAO(int x, int y, int z, FaceCullCache.FaceBasis basis, BlockShape.Vertex v) {
        int nx = x + basis.nx(), ny = y + basis.ny(), nz = z + basis.nz();

        int uCoord = v.x() * basis.ux() + v.y() * basis.uy() + v.z() * basis.uz();
        int vCoord = v.x() * basis.vx() + v.y() * basis.vy() + v.z() * basis.vz();

        int du = Integer.compare(uCoord, 64);
        int dv = Integer.compare(vCoord, 64);

        boolean side1 = du != 0 && !isTransparent(
                nx + du * basis.ux(), ny + du * basis.uy(), nz + du * basis.uz());
        boolean side2 = dv != 0 && !isTransparent(
                nx + dv * basis.vx(), ny + dv * basis.vy(), nz + dv * basis.vz());
        boolean corner = du != 0 && dv != 0 && !isTransparent(
                nx + du * basis.ux() + dv * basis.vx(),
                ny + du * basis.uy() + dv * basis.vy(),
                nz + du * basis.uz() + dv * basis.vz());

        return getAOIndex(side1, side2, corner);
    }

    // ------------------------------------------------------------------
    // Block storage / access
    // ------------------------------------------------------------------

    private int getIndex(int x, int y, int z) {
        return (z * SIZE_X * SIZE_Y) + (y * SIZE_X) + x;
    }

    public char getBlock(int x, int y, int z) {
        return blocks[getIndex(x, y, z)];
    }

    public void setBlock(int x, int y, int z, char block) {
        blocks[getIndex(x, y, z)] = block;
    }

    public boolean isTransparent(int x, int y, int z) {
        if (y < 0 || y >= SIZE_Y) return true;
        if (x < 0 || x >= SIZE_X || z < 0 || z >= SIZE_X) {
            char neighborBlock = getBlockInterchunk(x, y, z);
            if (neighborBlock == QuickAccessBlocks.NULL) return true; // unloaded chunk -> treat as transparent
            return neighborBlock == QuickAccessBlocks.AIR;
        }
        return blocks[getIndex(x, y, z)] == QuickAccessBlocks.AIR;
    }

    public char getBlockInterchunk(int x, int y, int z) {
        Chunk chunk = NanoCraft.WORLD.getChunk(worldPos.offset(Math.floorDiv(x, SIZE_X), Math.floorDiv(z, SIZE_Z)));
        if (chunk == null) return QuickAccessBlocks.NULL;
        return chunk.getBlock(Math.floorMod(x, SIZE_X), y, Math.floorMod(z, SIZE_Z));
    }

    public char getBlockChunkSafe(int x, int y, int z) {
        if (x < 0 || x >= SIZE_X || z < 0 || z >= SIZE_X) return QuickAccessBlocks.NULL;
        return blocks[getIndex(x, y, z)];
    }

    public void setBlockChunkSafe(int x, int y, int z, char block) {
        if (x < 0 || x >= SIZE_X || z < 0 || z >= SIZE_X) return;
        blocks[getIndex(x, y, z)] = block;
    }

    public void render() {
        if (mesh != null) mesh.render();
    }

    public void cleanup() {
        if (mesh != null) mesh.cleanup();
    }
}