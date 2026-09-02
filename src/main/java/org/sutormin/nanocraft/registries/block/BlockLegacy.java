package org.sutormin.nanocraft.registries.block;

public class BlockLegacy {
    public int top = 0;
    public int bottom = 0;
    public int north = 0;
    public int east = 0;
    public int south = 0;
    public int west = 0;
    private short id = 0;

    public BlockLegacy(int i){id = (short) i;}

    public short getId() {return id;}

    public BlockLegacy setTex(int t){
        top = t; bottom = t; north = t; east = t; south = t; west = t; return this;
    }
    public BlockLegacy setTop(int t){
        top = t; return this;
    }
    public BlockLegacy setBottom(int t){
        bottom = t; return this;
    }
    public BlockLegacy setNorth(int t){
        north = t; return this;
    }
    public BlockLegacy setEast(int t){
        east = t; return this;
    }
    public BlockLegacy setSouth(int t){
        south = t; return this;
    }
    public BlockLegacy setWest(int t){
        west = t; return this;
    }

}
