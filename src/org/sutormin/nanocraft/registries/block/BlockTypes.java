package org.sutormin.nanocraft.registries.block;

public class BlockTypes {
    public static short AIR = BlockTypeRegistry.add().getId();
    public static short DIRT = BlockTypeRegistry.add().setTextures(0b111111,0,0).getId();
    public static short GRASS = BlockTypeRegistry.add()
            .setTextures(0b100000,2,0)
            .setTextures(0b001111,1,0)
            .setTextures(0b010000,0,0).getId();
    public static short STONE = BlockTypeRegistry.add().setTextures(0b111111,7,0).getId();
    public static short OAK_LOG = BlockTypeRegistry.add()
            .setTextures(0b110000,3,0)
            .setTextures(0b001111,4,0).getId();
    public static short OAK_LEAVES = BlockTypeRegistry.add().setTextures(0b111111,5,0).getId();
    public static short OAK_PLANKS = BlockTypeRegistry.add().setTextures(0b111111,6,0).getId();
    public static short IRON_ORE = BlockTypeRegistry.add().setTextures(0b111111,0,1).getId();
    public static short COPPER_ORE = BlockTypeRegistry.add().setTextures(0b111111,1,1).getId();
    public static short GOLD_ORE = BlockTypeRegistry.add().setTextures(0b111111,2,1).getId();
    public static short REDSTONE_ORE = BlockTypeRegistry.add().setTextures(0b111111,3,1).getId();
    public static short DIAMOND_ORE = BlockTypeRegistry.add().setTextures(0b111111,4,1).getId();
}
