package org.sutormin.nanocraft.render;

public class Textures {
    public static Texture BLOCK = new Texture();

    public static int BLOCK_NOT_FOUND = BLOCK.setFallbackTexture("assets/texture/block/null.png");

    public static void loadTextures() {
        BLOCK.loadTextures();
    }
    
    public static void cleanup() {
        BLOCK.cleanup();
    }
}
