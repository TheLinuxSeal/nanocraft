package org.sutormin.nanocraft;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Chunk {
  public static final int ATLAS_SIZE = 8;
  public static final float TILE_SIZE = 1.0f / ATLAS_SIZE;
  public static final int SIZE_X = 16;
  public static final int SIZE_Y = 32;
  public static final int SIZE_Z = 16;

  public static final int SEA_LEVEL = 10;

  private final ChunkPos worldPos;
  private final byte[][][] blocks = new byte[SIZE_X][SIZE_Y][SIZE_Z];
  private Mesh mesh;

  public Chunk(ChunkPos worldPos) {
    this.worldPos = worldPos;
    generateTerrain();
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
            blocks[x][y][z] = 6; // stone
          } else if (y < height) {
            blocks[x][y][z] = 1; // dirt
          } else if (y == height) {
            if (height <= SEA_LEVEL + 1) {
              blocks[x][y][z] = 0; // sand (unimp)
            } else {
              blocks[x][y][z] = 2; // grass
            }
          } else if (y <= SEA_LEVEL) {
            blocks[x][y][z] = 0; // water (unimp)
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
      blocks[cx][y][cz] = 3;
    }
    int leafStart = startY + trunkHeight - 2;
    for (int lx = -1; lx <= 1; lx++) {
      for (int lz = -1; lz <= 1; lz++) {
        for (int ly = leafStart; ly <= leafStart + 2; ly++) {
          int bx = cx + lx;
          int bz = cz + lz;
          if (bx >= 0 && bx < SIZE_X && bz >= 0 && bz < SIZE_Z && ly < SIZE_Y) {
            if (blocks[bx][ly][bz] == 0) {
              blocks[bx][ly][bz] = 4; // leaves
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
          byte block = blocks[x][y][z];
          if (block == 0) continue;

          float wx = worldOffsetX + x;
          float wy = y;
          float wz = worldOffsetZ + z;

          if (isTransparent(x, y + 1, z)) addFace(vertices, indices, wx, wy, wz, 0, block); // top
          if (isTransparent(x, y - 1, z)) addFace(vertices, indices, wx, wy, wz, 1, block); // bottom
          if (isTransparent(x, y, z + 1)) addFace(vertices, indices, wx, wy, wz, 2, block); // front
          if (isTransparent(x, y, z - 1)) addFace(vertices, indices, wx, wy, wz, 3, block); // back
          if (isTransparent(x - 1, y, z)) addFace(vertices, indices, wx, wy, wz, 4, block); // left
          if (isTransparent(x + 1, y, z)) addFace(vertices, indices, wx, wy, wz, 5, block); // right
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

  private boolean isTransparent(int x, int y, int z) {
    if (x < 0 || x >= SIZE_X || y < 0 || y >= SIZE_Y || z < 0 || z >= SIZE_Z) return true;
    byte b = blocks[x][y][z];
    return b == 0 || b == 4;
  }

  private void addFace(List<Float> v, List<Integer> idx, float x, float y, float z, int face, byte block) {
    int startIndex = v.size() / 5;

    float[][] pos = switch (face) {
      case 0 -> new float[][]{{0,1,1}, {1,1,1}, {1,1,0}, {0,1,0}}; // top
      case 1 -> new float[][]{{0,0,0}, {1,0,0}, {1,0,1}, {0,0,1}}; // bottom
      case 2 -> new float[][]{{0,0,1}, {1,0,1}, {1,1,1}, {0,1,1}}; // front
      case 3 -> new float[][]{{1,0,0}, {0,0,0}, {0,1,0}, {1,1,0}}; // back
      case 4 -> new float[][]{{0,0,0}, {0,0,1}, {0,1,1}, {0,1,0}}; // left
      case 5 -> new float[][]{{1,0,1}, {1,0,0}, {1,1,0}, {1,1,1}}; // right
      default -> throw new IllegalArgumentException();
    };

    int tileX = 7;
    int tileY = 7;
    if (block == 1) {
      tileX = 0; tileY = 0;
    } else if (block == 2) {
      if (face == 0) {
        tileX = 2; tileY = 0;
      } else if (face == 1) {
        tileX = 0; tileY = 0;
      } else {
        tileX = 1; tileY = 0;
      }
    } else if (block == 3) {
      if (face == 0 || face == 1) {
        tileX = 3; tileY = 0;
      } else {
        tileX = 4; tileY = 0;
      }
    } else if (block == 4) {
      tileX = 5; tileY = 0;
    } else if (block == 5) {
      tileX = 6; tileY = 0;
    } else if (block == 6) {
      tileX = 7; tileY = 0;
    } else if (block == 7) {
      tileX = 0; tileY = 1;
    } else if (block == 8) {
      tileX = 1; tileY = 1;
    } else if (block == 9) {
      tileX = 2; tileY = 1;
    } else if (block == 10) {
      tileX = 3; tileY = 1;
    } else if (block == 11) {
      tileX = 4; tileY = 1;
    }

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
      rot = hash(bx, by, bz) % 3;
    }

    for (int i = 0; i < 4; i++) {
      int uvIndex = (i + rot) % 4; 

      v.add(x + pos[i][0]);
      v.add(y + pos[i][1]);
      v.add(z + pos[i][2]);
      
      v.add(uvs[uvIndex][0]);
      v.add(uvs[uvIndex][1]);
    }

    idx.add(startIndex);
    idx.add(startIndex + 1);
    idx.add(startIndex + 2);
    idx.add(startIndex + 2);
    idx.add(startIndex + 3);
    idx.add(startIndex);
  }

  public byte getBlock(int x, int y, int z) {
    return blocks[x][y][z];
  }

  public void setBlock(int x, int y, int z, byte block) {
    blocks[x][y][z] = block;
  }

  public void render() {
    if (mesh != null) mesh.render();
  }

  public void cleanup() {
    if (mesh != null) mesh.cleanup();
  }
}