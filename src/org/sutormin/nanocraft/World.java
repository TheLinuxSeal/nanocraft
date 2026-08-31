package org.sutormin.nanocraft;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class World {
  private final Map<ChunkPos, Chunk> chunks = new HashMap<>();

  public World() {
  }

  public void loadChunk(ChunkPos pos) {
    if (chunks.containsKey(pos)) return;
    Chunk chunk = new Chunk(pos);
    chunk.buildMesh();
    chunks.put(pos, chunk);
  }

  public void rebuildChunk(ChunkPos pos) {
    if (!chunks.containsKey(pos)) return;
    Chunk chunk = chunks.get(pos);
    chunk.buildMesh();
  }

  public void unloadChunk(ChunkPos pos) {
    if (!chunks.containsKey(pos)) return;
    Chunk chunk = chunks.get(pos);
    chunk.cleanup();
    chunks.remove(pos);
  }

  public void loadChunksAndUnloadAllOtherChunks(Collection<ChunkPos> posList) {
    Set<ChunkPos> keepSet = new HashSet<>(posList);

    chunks.entrySet().removeIf(entry -> {
      if (!keepSet.contains(entry.getKey())) {
        entry.getValue().cleanup();
        return true;
      }
      return false;
    });

    for (ChunkPos pos : posList) {
      loadChunk(pos);
    }
  }

  public byte getBlockAt(int x, int y, int z) {
    ChunkPos chunkPos = getChunkPosFromBlock(x, z);
    Chunk chunk = chunks.get(chunkPos);
    if (chunk == null) return 0;

    int localX = Math.floorMod(x, Chunk.SIZE_X);
    int localZ = Math.floorMod(z, Chunk.SIZE_Z);

    return chunk.getBlock(localX, y, localZ);
  }

  public void setBlockAt(int x, int y, int z, byte block) {
    ChunkPos chunkPos = getChunkPosFromBlock(x, z);
    Chunk chunk = chunks.get(chunkPos);
    if (chunk == null) return;

    int localX = Math.floorMod(x, Chunk.SIZE_X);
    int localZ = Math.floorMod(z, Chunk.SIZE_Z);

    chunk.setBlock(localX, y, localZ, block);
    chunk.buildMesh();

    if (localX == 0) rebuildChunk(new ChunkPos(chunkPos.x() - 1, chunkPos.z()));
    if (localX == Chunk.SIZE_X - 1) rebuildChunk(new ChunkPos(chunkPos.x() + 1, chunkPos.z()));
    if (localZ == 0) rebuildChunk(new ChunkPos(chunkPos.x(), chunkPos.z() - 1));
    if (localZ == Chunk.SIZE_Z - 1) rebuildChunk(new ChunkPos(chunkPos.x(), chunkPos.z() + 1));
  }

  public ChunkPos getChunkPosFromBlock(int x, int z) {
    int chunkX = Math.floorDiv(x, Chunk.SIZE_X);
    int chunkZ = Math.floorDiv(z, Chunk.SIZE_Z);
    return new ChunkPos(chunkX, chunkZ);
  }

  public void render() {
    for (Chunk chunk : chunks.values()) {
      chunk.render();
    }
  }

  public void cleanup() {
    for (Chunk chunk : chunks.values()) {
      chunk.cleanup();
    }
    chunks.clear();
  }
}