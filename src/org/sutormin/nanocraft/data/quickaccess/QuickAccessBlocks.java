package org.sutormin.nanocraft.data.quickaccess;

import org.sutormin.nanocraft.data.Registries;

public class QuickAccessBlocks {
    public static char NULL;
    public static char NOT_FOUND;
    public static char AIR;
    public static char STONE;
    public static void loadFromRegistry(){
        NULL = (char) Registries.BLOCK.get("null").getId();
        NOT_FOUND = (char) Registries.BLOCK.get("not_found").getId();
        AIR = (char) Registries.BLOCK.get("air").getId();
        STONE = (char) Registries.BLOCK.get("stone").getId();
    }
}
