package org.sutormin.nanocraft.networking.packets.config.c2s;

import io.netty.buffer.ByteBuf;
import org.sutormin.nanocraft.networking.coders.PacketIO;

public class KnownPacksResponse {
    public static short ID = 7;
    public static void make(ByteBuf buf, ByteBuf data) {
        PacketIO.write(buf, ID, data);
    }
}
