package org.sutormin.nanocraft.resources.texture;

public class Textures {
    public static Texture BLOCK = new Texture();

    public static int BLOCK_NOT_FOUND = BLOCK.addTexture("assets/texture/block/not_found.png");

    public static void loadTextures() {
        BLOCK.loadTextures();
    }
    
    public static void cleanup() {
        BLOCK.cleanup();
    }
}
