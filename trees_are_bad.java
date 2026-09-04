

    public void setBlockInterchunk(int x, int y, int z, short block) {
        Chunk chunk = NanoCraft.WORLD.getChunk(worldPos.offset(Math.floorDiv(x, SIZE_X), Math.floorDiv(z, SIZE_Z)));
        if (chunk == null) return;
        chunk.setBlock(Math.floorMod(x, SIZE_X), y, Math.floorMod(z, SIZE_Z), block);
    }

    public void setBlockInterchunkPromise(int x, int y, int z, Function<Short,Short> block) {
        if (x < 0 || x >= SIZE_X || z < 0 || z >= SIZE_X) {
            Chunk chunk = NanoCraft.WORLD.getChunk(worldPos.offset(Math.floorDiv(x, SIZE_X), Math.floorDiv(z, SIZE_Z)));
            if (chunk == null) NanoCraft.WORLD.setBlockPromise(worldPos.x()*SIZE_X+x,y,worldPos.z()*SIZE_Z+z,block);
            else {
                short b = block.apply(chunk.getBlock(Math.floorMod(x, SIZE_X), y, Math.floorMod(z, SIZE_Z)));
                if (b != BlockTypes.NULL) chunk.setBlock(Math.floorMod(x, SIZE_X), y, Math.floorMod(z, SIZE_Z), b);
            }
        } else {
            short b = block.apply(blocks[getIndex(x,y,z)]);
            if (b != BlockTypes.NULL)  blocks[getIndex(x,y,z)] = b;
        }
    }


    private void generateTree(int cx, int startY, int cz) {
        int trunkHeight = 4;
        for (int y = startY; y < startY + trunkHeight && y < SIZE_Y; y++) {
            blocks[getIndex(cx, y, cz)] = BlockTypes.OAK_LOG;
        }
        int leafStart = startY + trunkHeight - 2;
        Function<Short,Short> leafFunc = block -> {if (block == BlockTypes.AIR) return BlockTypes.OAK_LEAVES;else return BlockTypes.NULL;};
        for (int lx = -2; lx <= 2; lx++) {
            for (int lz = -2; lz <= 2; lz++) {
                setBlockInterchunkPromise(cx+lx,leafStart,cz+lz,leafFunc);
            }
        }
        for (int lx = -2; lx <= 2; lx++) {
            for (int lz = -2; lz <= 2; lz++) {
                setBlockInterchunkPromise(cx+lx,leafStart+1,cz+lz,leafFunc);
            }
        }
        for (int lx = -1; lx <= 1; lx++) {
            for (int lz = -1; lz <= 1; lz++) {
                setBlockInterchunkPromise(cx+lx,leafStart+2,cz+lz,leafFunc);
            }
        }
        setBlockInterchunkPromise(cx,leafStart+3,cz,leafFunc);
        setBlockInterchunkPromise(cx-1,leafStart+3,cz,leafFunc);
        setBlockInterchunkPromise(cx+1,leafStart+3,cz,leafFunc);
        setBlockInterchunkPromise(cx,leafStart+3,cz-1,leafFunc);
        setBlockInterchunkPromise(cx,leafStart+3,cz+1,leafFunc);
    }

                if (height > SEA_LEVEL + 1 && hash(wx, wz) % 100 == 0 && x > 1 && z > 1 && x < SIZE_X - 1 && z < SIZE_Z - 1) {
                    generateTree(x, height + 1, z);
                }