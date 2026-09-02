package org.sutormin.nanocraft;

public record ChunkPos(int x, int z) {
    public ChunkPos offset(int ox, int oz) {
        return new ChunkPos(x+ox,z+oz);
    }
}