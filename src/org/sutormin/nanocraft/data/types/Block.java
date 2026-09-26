package org.sutormin.nanocraft.data.types;

import org.sutormin.nanocraft.data.registry.RegistryType;

public class Block extends RegistryType {
    private BlockShape shape;

    public Block(int id) {super(id);}
    @Override
    public RegistryType copy(int newId) {
        Block b = new Block(newId);
        b.setShape(getShape());
        return b;
    }

    public BlockShape getShape() {return shape;}
    public Block setShape(BlockShape shape) {this.shape = shape;return this;}

    public int getTexture(int which){
        return 0; // TODO: FIX
    }
}
