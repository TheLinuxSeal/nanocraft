package org.sutormin.nanocraft;

import org.sutormin.nanocraft.registries.block.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Chunk {
    public static final int ATLAS_SIZE = 8;
    public static final float TILE_SIZE = 1.0f / ATLAS_SIZE;
    public static final int SIZE_X = 16;
    public static final int SIZE_Y = 32;
    public static final int SIZE_Z = 16;

    public static final int SEA_LEVEL = 10;

    private final ChunkPos worldPos;
    // 32 bits for: 16b = blockid, 16b = blockstate (redstone level, orientation, etc)
    private final short[] blocks = new short[SIZE_X * SIZE_Y * SIZE_Z];
    public Mesh mesh;

    public Chunk(ChunkPos worldPos) {
        this.worldPos = worldPos;
        generateTerrain();
        /*Random rand = new Random();

        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                int height = 16;
                for (int y = 0; y < height - 4; y++) {
                    blocks[getId(x,y,z)] = BlockTypes.STONE;
                }
                for (int y = height - 4; y < height; y++) {
                    blocks[getId(x,y,z)] = BlockTypes.DIRT;
                }
                List a = List.of(
                        BlockTypes.AIR,
                        BlockTypes.AIR,
                        BlockTypes.AIR,
                        BlockTypes.AIR,
                        BlockTypes.AIR,
                        BlockTypes.AIR,
                        BlockTypes.AIR,
                        BlockTypes.AIR,
                        BlockTypes.DIRT,
                        BlockTypes.GRASS,
                        BlockTypes.STONE,
                        BlockTypes.OAK_LOG,
                        BlockTypes.OAK_LEAVES,
                        BlockTypes.OAK_PLANKS,
                        BlockTypes.IRON_ORE,
                        BlockTypes.COPPER_ORE,
                        BlockTypes.GOLD_ORE,
                        BlockTypes.REDSTONE_ORE,
                        BlockTypes.DIAMOND_ORE

                );
                blocks[getId(x,height,z)] = (short) a.get(rand.nextInt(a.size()));
            }
        }*/
    }

    private void generateTerrain() {
        int worldOffsetX = worldPos.x() * SIZE_X;
        int worldOffsetZ = worldPos.z() * SIZE_Z;

        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                int wx = worldOffsetX + x;
                int wz = worldOffsetZ + z;

                float baseNoise = sampleNoise2D(wx * 0.02f, wz * 0.02f);
                float detailNoise = sampleNoise2D(wx * 0.08f, wz * 0.08f);

                int height = (int) (14 + (baseNoise * 8.0f) + (detailNoise * 3.0f));
                height = Math.max(1, Math.min(SIZE_Y - 1, height));

                for (int y = 0; y < SIZE_Y; y++) {
                    if (y < height - 4) {
                        blocks[getId(x,y,z)] = BlockTypes.STONE; // stone
                    } else if (y < height) {
                        blocks[getId(x,y,z)] = BlockTypes.DIRT; // dirt
                    } else if (y == height) {
                        if (height <= SEA_LEVEL + 1) {
                            blocks[getId(x,y,z)] = BlockTypes.GOLD_ORE; // sand (unimp)
                        } else {
                            blocks[getId(x,y,z)] = BlockTypes.GRASS; // grass
                        }
                    } else if (y <= SEA_LEVEL) {
                        blocks[getId(x,y,z)] = BlockTypes.DIAMOND_ORE; // water (unimp)
                    }
                }

                if (height > SEA_LEVEL + 1 && hash(wx, wz) % 100 == 0 && x > 1 && z > 1 && x < SIZE_X - 1 && z < SIZE_Z - 1) {
                    generateTree(x, height + 1, z);
                }
            }
        }
    }

    private void generateTree(int cx, int startY, int cz) {
        int trunkHeight = 4;
        for (int y = startY; y < startY + trunkHeight && y < SIZE_Y; y++) {
            blocks[getId(cx,y,cz)] = BlockTypes.OAK_LOG;
        }
        int leafStart = startY + trunkHeight - 2;
        for (int lx = -1; lx <= 1; lx++) {
            for (int lz = -1; lz <= 1; lz++) {
                for (int ly = leafStart; ly <= leafStart + 2; ly++) {
                    int bx = cx + lx;
                    int bz = cz + lz;
                    if (bx >= 0 && bx < SIZE_X && bz >= 0 && bz < SIZE_Z && ly < SIZE_Y) {
                        if (blocks[getId(bx,ly,bz)] == BlockTypes.AIR) {
                            blocks[getId(bx,ly,bz)] = BlockTypes.OAK_LEAVES; // leaves
                        }
                    }
                }
            }
        }
    }

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

    public void buildMesh() {
        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        int worldOffsetX = worldPos.x() * SIZE_X;
        int worldOffsetZ = worldPos.z() * SIZE_Z;

        for (int x = 0; x < SIZE_X; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                for (int z = 0; z < SIZE_Z; z++) {
                    if (blocks[getId(x,y,z)] == 0) continue;
                    BlockType block = BlockTypeRegistry.getBlock(blocks[getId(x,y,z)]);

                    float wx = worldOffsetX + x;
                    float wy = y;
                    float wz = worldOffsetZ + z;

                    if (isAir(x, y + 1, z)) addFace(vertices, indices, wx, wy, wz, 0, block.getTexture(0)); // top
                    if (isAir(x, y - 1, z)) addFace(vertices, indices, wx, wy, wz, 1, block.getTexture(1)); // bottom
                    if (isAir(x, y, z + 1)) addFace(vertices, indices, wx, wy, wz, 2, block.getTexture(2)); // front
                    if (isAir(x, y, z - 1)) addFace(vertices, indices, wx, wy, wz, 3, block.getTexture(3)); // back
                    if (isAir(x - 1, y, z)) addFace(vertices, indices, wx, wy, wz, 4, block.getTexture(4)); // left
                    if (isAir(x + 1, y, z)) addFace(vertices, indices, wx, wy, wz, 5, block.getTexture(5)); // right
                }
            }
        }

        float[] vArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) vArray[i] = vertices.get(i);

        int[] iArray = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) iArray[i] = indices.get(i);

        if (mesh != null) mesh.cleanup();
        mesh = new Mesh(vArray, iArray);
    }

    private boolean isAir(int x, int y, int z) {
        if (y < 0 || y >= SIZE_Y) return true;

        if (x < 0) return getBlockFromOffsetChunk(-1,0,SIZE_X+x,y,z)==BlockTypes.AIR;
        if (x >= SIZE_Z)  return getBlockFromOffsetChunk(1,0,x%SIZE_X,y,z)==BlockTypes.AIR;
        if (z < 0) return getBlockFromOffsetChunk(0,-1,x,y,SIZE_Z+z)==BlockTypes.AIR;
        if (z >= SIZE_Z)  return getBlockFromOffsetChunk(0,1,x,y,z%SIZE_Z)==BlockTypes.AIR;
        return blocks[getId(x,y,z)] == BlockTypes.AIR;
    }

    public short getBlockFromOffsetChunk(int cx, int cz, int x, int y, int z) {
        Chunk chunk = NanoCraft.world.getChunk(worldPos.offset(cx,cz));
        if (chunk == null) return -1;
        return chunk.getBlock(x,y,z);
    }

    private void addFace(List<Float> v, List<Integer> idx, float x, float y, float z, int face, int block) {
        int startIndex = v.size() / 6;

        float[][] pos = switch (face) {
            case 0 -> new float[][]{{0,1,1}, {1,1,1}, {1,1,0}, {0,1,0}}; // top
            case 1 -> new float[][]{{0,0,0}, {1,0,0}, {1,0,1}, {0,0,1}}; // bottom
            case 2 -> new float[][]{{0,0,1}, {1,0,1}, {1,1,1}, {0,1,1}}; // front
            case 3 -> new float[][]{{1,0,0}, {0,0,0}, {0,1,0}, {1,1,0}}; // back
            case 4 -> new float[][]{{0,0,0}, {0,0,1}, {0,1,1}, {0,1,0}}; // left
            case 5 -> new float[][]{{1,0,1}, {1,0,0}, {1,1,0}, {1,1,1}}; // right
            default -> throw new IllegalArgumentException();
        };

        int tileX = block & 0xFFFF;
        int tileY = (block >>> 16) & 0xFFFF;

        float uMin = tileX * TILE_SIZE;
        float uMax = uMin + TILE_SIZE;
        float vMin = 1.0f - ((tileY + 1) * TILE_SIZE);
        float vMax = 1.0f - (tileY * TILE_SIZE);


        float[][] uvs = {
                {uMin, vMin},
                {uMax, vMin},
                {uMax, vMax},
                {uMin, vMax}
        };

        int rot = 0;
        if ((tileX == 0 && tileY == 0) || (tileX == 2 && tileY == 0)) {
            int bx = (int) Math.floor(x) + worldPos.x();
            int by = (int) Math.floor(y);
            int bz = (int) Math.floor(z) + worldPos.z();

            int hash = bx * 73856093 ^ by * 19349663 ^ bz * 83492791;
            hash ^= hash >>> 16;
            hash *= 0x85ebca6b;
            hash ^= hash >>> 13;
            hash *= 0xc2b2ae35;
            hash ^= hash >>> 16;
            rot = (hash & Integer.MAX_VALUE) & 3;
        }

        for (int i = 0; i < 4; i++) {
            int uvIndex = (i + rot) % 4;

            v.add(x + pos[i][0]);
            v.add(y + pos[i][1]);
            v.add(z + pos[i][2]);

            v.add(uvs[uvIndex][0]);
            v.add(uvs[uvIndex][1]);
            v.add(0f);
        }

        idx.add(startIndex);
        idx.add(startIndex + 1);
        idx.add(startIndex + 2);
        idx.add(startIndex + 2);
        idx.add(startIndex + 3);
        idx.add(startIndex);
    }

    private int getId(int x, int y, int z){
        return (z * SIZE_X * SIZE_Y) + (y * SIZE_X) + x;
    }

    public short getBlock(int x, int y, int z) {
        return blocks[getId(x,y,z)];
    }

    public void setBlock(int x, int y, int z, short block) {
        blocks[getId(x,y,z)] = block;
    }

    public void render() {
        if (mesh != null) mesh.render();
    }

    public void cleanup() {
        if (mesh != null) mesh.cleanup();
    }
}