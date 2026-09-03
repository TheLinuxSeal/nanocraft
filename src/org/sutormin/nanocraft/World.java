package org.sutormin.nanocraft;

import org.sutormin.nanocraft.block.BlockTypes;

import java.util.*;

public class World {
    private final Map<ChunkPos, Chunk> chunks = new HashMap<>();
    private final List<ChunkPos> forceRemeshChunks = new ArrayList<>();
    private final List<ChunkPos> cancelRemeshChunks = new ArrayList<>();

    public World() {
    }

    public void makeChunk(ChunkPos pos) {
        if (chunks.containsKey(pos)) return;
        Chunk chunk = new Chunk(pos);
        //chunk.buildMesh();
        chunks.put(pos, chunk);
        forceRemeshChunks.add(pos.offset(-1,0));
        forceRemeshChunks.add(pos.offset(1,0));
        forceRemeshChunks.add(pos.offset(0,-1));
        forceRemeshChunks.add(pos.offset(0,1));
        cancelRemeshChunks.add(pos);
    }

    public void meshChunk(ChunkPos pos) {
        if (!chunks.containsKey(pos)) return;
        Chunk chunk = chunks.get(pos);
        if (chunk.mesh != null && (!forceRemeshChunks.contains(pos) || cancelRemeshChunks.contains(pos))) return;
        chunk.buildMesh();
    }

    public void removeChunk(ChunkPos pos) {
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

        forceRemeshChunks.clear();
        cancelRemeshChunks.clear();

        for (ChunkPos pos : posList) {
            makeChunk(pos);
        }

        for (ChunkPos pos : posList) {
            meshChunk(pos);
        }
    }

    public int getBlockAt(int x, int y, int z) {
        ChunkPos chunkPos = getChunkPosFromBlock(x, z);
        Chunk chunk = chunks.get(chunkPos);
        if (chunk == null) return BlockTypes.AIR;

        System.out.println(x + ", " + y);
        System.out.println(chunkPos);
        System.out.println();

        int localX = Math.floorMod(x, Chunk.SIZE_X);
        int localZ = Math.floorMod(z, Chunk.SIZE_Z);

        return chunk.getBlock(localX, y, localZ);
    }

    public void setBlockAt(int x, int y, int z, short block) {
        ChunkPos chunkPos = getChunkPosFromBlock(x, z);
        Chunk chunk = chunks.get(chunkPos);
        if (chunk == null) return;

        int localX = Math.floorMod(x, Chunk.SIZE_X);
        int localZ = Math.floorMod(z, Chunk.SIZE_Z);


        chunk.setBlock(localX, y, localZ, block);
        chunk.buildMesh();

        if (localX == 0) meshChunk(new ChunkPos(chunkPos.x() - 1, chunkPos.z()));
        if (localX == Chunk.SIZE_X - 1) meshChunk(new ChunkPos(chunkPos.x() + 1, chunkPos.z()));
        if (localZ == 0) meshChunk(new ChunkPos(chunkPos.x(), chunkPos.z() - 1));
        if (localZ == Chunk.SIZE_Z - 1) meshChunk(new ChunkPos(chunkPos.x(), chunkPos.z() + 1));
    }

    public Chunk getChunk(ChunkPos pos){
        return chunks.get(pos);
    }

    public ChunkPos getChunkPosFromBlock(int x, int z) {
        int chunkX = Math.floorDiv(x, Chunk.SIZE_X);
        int chunkZ = Math.floorDiv(z, Chunk.SIZE_Z);
        return new ChunkPos(chunkX, chunkZ);
    }

    public void renderChunks() {
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