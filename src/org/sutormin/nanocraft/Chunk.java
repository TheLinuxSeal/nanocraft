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

  private final ChunkPos worldPos;
  private final byte[][][] blocks = new byte[SIZE_X][SIZE_Y][SIZE_Z];
  private Mesh mesh;

  public Chunk(ChunkPos worldPos) {
    this.worldPos = worldPos;

    for (int x = 0; x < SIZE_X; x++) {
      for (int z = 0; z < SIZE_Z; z++) {
        int height = 16;
        for (int y = 0; y < height - 4; y++) {
          blocks[x][y][z] = 6;
        }
        for (int y = height - 4; y < height; y++) {
          blocks[x][y][z] = 1;
        }
        blocks[x][height][z] = 2;
      }
    }
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

          if (isAir(x, y + 1, z)) addFace(vertices, indices, wx, wy, wz, 0, block); // top
          if (isAir(x, y - 1, z)) addFace(vertices, indices, wx, wy, wz, 1, block); // bottom
          if (isAir(x, y, z + 1)) addFace(vertices, indices, wx, wy, wz, 2, block); // front
          if (isAir(x, y, z - 1)) addFace(vertices, indices, wx, wy, wz, 3, block); // back
          if (isAir(x - 1, y, z)) addFace(vertices, indices, wx, wy, wz, 4, block); // left
          if (isAir(x + 1, y, z)) addFace(vertices, indices, wx, wy, wz, 5, block); // right
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
    if (x < 0 || x >= SIZE_X || y < 0 || y >= SIZE_Y || z < 0 || z >= SIZE_Z) return true;
    return blocks[x][y][z] == 0;
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