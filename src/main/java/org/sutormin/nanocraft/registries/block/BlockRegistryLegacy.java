package org.sutormin.nanocraft.registries.block;

import org.sutormin.nanocraft.registries.Registry;

public class BlockRegistryLegacy {
    public Registry<BlockLegacy> registry = new Registry<>(BlockLegacy::new);
    public BlockLegacy AIR = registry.add(); //HAS to be added first for a id of 0
    public short DIRT = registry.add().setTex(0).getId();
    public short GRASS = registry.add().setTex(1).setTop(2).setBottom(0).getId();
    public short STONE = registry.add().setTex(7).getId();
}
