package org.sutormin.nanocraft.resources;

public class Textures {
    public static Texture BLOCK = new Texture();
    public static void loadTextures(){
        BLOCK.loadTextures();
    }
    public static void cleanup(){
        BLOCK.cleanup();
    }
}
