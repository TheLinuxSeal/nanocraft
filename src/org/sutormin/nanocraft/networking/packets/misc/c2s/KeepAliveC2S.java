package org.sutormin.nanocraft.networking.packets.misc.c2s;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.sutormin.nanocraft.networking.coders.PacketIO;
import org.sutormin.nanocraft.networking.coders.VarCoder;

public class KeepAliveC2S {
    public static short ID = 4;
    public static void make(ByteBuf buf, long id) {
        ByteBuf packet = Unpooled.buffer();
        VarCoder.writeVarLong(packet,id);
        PacketIO.write(buf, ID, packet);
        packet.release();
    }
}
