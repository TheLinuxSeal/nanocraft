package org.sutormin.nanocraft.registries.block;

import java.util.ArrayList;
import java.util.List;

public class BlockTypeRegistry {
    static private final List<BlockType> blockTypes = new ArrayList<>();
    static public BlockType add(){
        BlockType block = new BlockType(blockTypes.size());
        blockTypes.add(block);
        return block;
    };
    static public BlockType getBlock(int id){
        return blockTypes.get(id);
    }
}