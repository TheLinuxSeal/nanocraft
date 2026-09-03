package org.sutormin.nanocraft.block;

import java.util.ArrayList;
import java.util.List;

public class BlockRegistry {
    private static final List<BlockType> blockTypes = new ArrayList<>();
    public static BlockType add(){
        BlockType block = new BlockType(blockTypes.size());
        blockTypes.add(block);
        return block;
    };
    public static BlockType getBlock(int id){
        return blockTypes.get(id);
    }
}