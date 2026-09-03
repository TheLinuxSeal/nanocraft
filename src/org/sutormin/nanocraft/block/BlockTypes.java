package org.sutormin.nanocraft.block;

public class BlockTypes {
    public static short AIR;
    public static short DIRT;
    public static short GRASS;
    public static short SAND;
    public static short STONE;
    public static short OAK_LOG;
    public static short OAK_LEAVES;
    public static short OAK_PLANKS;
    public static short WATER;
    public static short IRON_ORE;
    public static short COPPER_ORE;
    public static short GOLD_ORE;
    public static short REDSTONE_ORE;
    public static short DIAMOND_ORE;

    public static void define(){
        AIR = BlockRegistry.add().getId();
        DIRT = BlockRegistry.add().setTextures("dirt.png").getId();
        GRASS = BlockRegistry.add()
                .setTextures(0b100000,"grass_block_top.png")
                .setTextures(0b001111,"grass_block_side.png")
                .setTextures(0b010000,"dirt.png").getId();
        SAND = BlockRegistry.add().setTextures("sand.png").getId();
        STONE = BlockRegistry.add().setTextures("stone.png").getId();
        OAK_LOG = BlockRegistry.add()
                .setTextures(0b110000,"oak_log_top.png")
                .setTextures(0b001111,"oak_log.png").getId();
        OAK_LEAVES = BlockRegistry.add().setTextures("oak_leaves.png").getId();
        OAK_PLANKS = BlockRegistry.add().setTextures("oak_planks.png").getId();
        IRON_ORE = BlockRegistry.add().setTextures("iron_ore.png").getId();
        COPPER_ORE = BlockRegistry.add().setTextures("copper_ore.png").getId();
        GOLD_ORE = BlockRegistry.add().setTextures("gold_ore.png").getId();
        REDSTONE_ORE = BlockRegistry.add().setTextures("redstone_ore.png").getId();
        DIAMOND_ORE = BlockRegistry.add().setTextures("diamond_ore.png").getId();
        WATER = BlockRegistry.add().setTextures("water.png").getId();
    }
}
