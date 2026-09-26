package org.sutormin.nanocraft.data.definitions;

import org.sutormin.nanocraft.data.Registries;
import org.sutormin.nanocraft.data.registry.Registry;
import org.sutormin.nanocraft.data.types.Block;

public class BlockDefinitions {
    private static final String[] WOOD_TYPES = {"oak","spruce","dark_oak","birch","acacia","pale_oak","poplar"};
    private static final String[] NETHER_WOOD_TYPES = {"crimson","warped"};
    private static final String[] STONE_TYPES = {"stone","cobblestone","andesite","granite","diorite"};
    private static final String[] CUTTABLE_STONE_TYPES = {"deepslate","tuff",""};
    public static void define(Registry<Block> reg){
        reg.addNew("null");
        reg.addNew("not_found");
        reg.addNew("air");
        for (String x : WOOD_TYPES){
            reg.addNew(x+"_log");
        }

    }
}
