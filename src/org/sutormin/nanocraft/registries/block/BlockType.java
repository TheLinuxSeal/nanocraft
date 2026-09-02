package org.sutormin.nanocraft.registries.block;

import java.util.Map;

public class BlockType {
    private Map<BlockStateKey, Short> blockState = Map.of();
    private final int[] textures = new int[6];
    private String name;
    private short id;

    public BlockType(int id){
        this.id = (short) id;
    }

    public BlockType setTextures(int which, int u, int v){
        if ((which & 1)!=0) {textures[5]=u+v*65536;}
        if ((which & 2)!=0) {textures[4]=u+v*65536;}
        if ((which & 4)!=0) {textures[3]=u+v*65536;}
        if ((which & 8)!=0) {textures[2]=u+v*65536;}
        if ((which & 16)!=0) {textures[1]=u+v*65536;}
        if ((which & 32)!=0) {textures[0]=u+v*65536;}
        return this;
    }

    public int getTexture(int which){return textures[which];}

    public short getId() {
        return id;
    }

    public String getName() {
        return name;
    }

}
