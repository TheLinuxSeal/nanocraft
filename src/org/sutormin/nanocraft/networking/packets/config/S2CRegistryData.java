package org.sutormin.nanocraft.networking.packets.config;

import io.netty.buffer.ByteBuf;
import org.sutormin.nanocraft.networking.coders.NbtSkipper;
import org.sutormin.nanocraft.networking.coders.VarCoder;
import org.sutormin.nanocraft.networking.packets.types.S2CPacket;
import org.sutormin.nanocraft.world.biome.BiomeTint;

import java.util.ArrayList;
import java.util.List;

/**
 * One registry's entries, in id order. Only the biome registry is used: its order gives the biome
 * ids in chunk data. Entries from packs we echoed back as known come without data; others carry
 * NBT, which is skipped.
 */
public class S2CRegistryData implements S2CPacket {
    @Override
    public void read(ByteBuf buf) {
        String registry = VarCoder.readString(buf);
        if (!registry.equals("minecraft:worldgen/biome")) return;

        int count = VarCoder.readVarInt(buf);
        List<String> names = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            names.add(VarCoder.readString(buf));
            if (buf.readBoolean()) NbtSkipper.skipRoot(buf);
        }
        BiomeTint.setRegistry(names);
    }
}
