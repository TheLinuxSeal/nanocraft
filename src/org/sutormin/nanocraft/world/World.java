package org.sutormin.nanocraft.world;

import org.sutormin.nanocraft.NanoCraft;
import org.sutormin.nanocraft.block.BlockTypes;

import java.util.*;
import java.util.function.Function;

public class World {
    private final Map<ChunkPos, Chunk> chunks = new HashMap<>();

    public World() {
    }

    public void removeChunk(ChunkPos pos) {
        if (!chunks.containsKey(pos)) return;
        Chunk chunk = chunks.get(pos);
        chunk.cleanup();
        chunks.remove(pos);
    }

    public void addChunk(ChunkPos pos, Chunk chunk) {
        Chunk old = chunks.put(pos, chunk);
        if (old != null) old.cleanup();
        chunk.buildMesh();
        remesh(pos.offset(-1, 0));
        remesh(pos.offset(1, 0));
        remesh(pos.offset(0, -1));
        remesh(pos.offset(0, 1));
    }

    private void remesh(ChunkPos pos) {
        Chunk c = chunks.get(pos);
        if (c != null) c.buildMesh();
    }

    public void drainNetworkChunks(int budget) {
        ChunkLoader.Pending p;
        while (budget-- > 0 && (p = ChunkLoader.poll()) != null) {
            Chunk c = new Chunk(p.pos());
            c.setBlocks(p.blocks());
            addChunk(p.pos(), c);
        }
    }


    public int chunkCount() {
        return chunks.size();
    }

    public char getBlockAt(int x, int y, int z) {
        ChunkPos chunkPos = getChunkPosFromBlock(x, z);
        Chunk chunk = chunks.get(chunkPos);
        if (chunk == null) return BlockTypes.AIR;

        int localX = Math.floorMod(x, Chunk.SIZE_X);
        int localZ = Math.floorMod(z, Chunk.SIZE_Z);

        return chunk.getBlock(localX, y, localZ);
    }

    public void setBlockAt(int x, int y, int z, char block) {
        ChunkPos chunkPos = getChunkPosFromBlock(x, z);
        Chunk chunk = chunks.get(chunkPos);
        if (chunk == null) return;

        int localX = Math.floorMod(x, Chunk.SIZE_X);
        int localZ = Math.floorMod(z, Chunk.SIZE_Z);


        chunk.setBlock(localX, y, localZ, block);
        chunk.buildMesh();

        if (localX == 0) remesh(chunkPos.offset(-1,0));
        if (localX == Chunk.SIZE_X - 1) remesh(chunkPos.offset(1,0));
        if (localZ == 0) remesh(chunkPos.offset(0,-1));
        if (localZ == Chunk.SIZE_Z - 1) chunkPos.offset(0,1);
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