package org.sutormin.nanocraft.registries.block;

public class BlockTypes {
    public static short AIR = BlockTypeRegistry.add().getId();
    public static short DIRT = BlockTypeRegistry.add().setTextures(0b111111,0,0).getId();
    public static short GRASS = BlockTypeRegistry.add()
            .setTextures(0b100000,2,0)
            .setTextures(0b001111,1,0)
            .setTextures(0b010000,0,0).getId();
    public static short STONE = BlockTypeRegistry.add().setTextures(0b111111,7,0).getId();
}
