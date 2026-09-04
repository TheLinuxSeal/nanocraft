package org.sutormin.nanocraft.block;

import org.sutormin.nanocraft.resources.Textures;

public class BlockType {
    private final short id;
    private String name;
    private final int[] textures = new int[6];

    public BlockType(int id) {
        this.id = (short) id;
    }

    public BlockType setTextures(int which, String path){
        int t = Textures.BLOCK.addTexture("texture/block/" + path);
        if ((which & 1) != 0) textures[5] = t;
        if ((which & 2) != 0) textures[4] = t;
        if ((which & 4) != 0) textures[3] = t;
        if ((which & 8) != 0) textures[2] = t;
        if ((which & 16) != 0) textures[1] = t;
        if ((which & 32) != 0) textures[0] = t;
        return this;
    }

    public BlockType setTextures(String path) {
        int t = Textures.BLOCK.addTexture("texture/block/" + path);
        for (var i = 0; i < 6; i++) {
            textures[i] = t;
        }
        return this;
    }

    public int getTexture(int which) {
        return textures[which];
    }

    public short getId() {
        return id;
    }

    public String getName() {
        return name;
    }

}
