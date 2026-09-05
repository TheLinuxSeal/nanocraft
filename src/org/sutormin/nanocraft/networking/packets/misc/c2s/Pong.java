package org.sutormin.nanocraft.networking.packets.misc.c2s;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.sutormin.nanocraft.networking.coders.PacketIO;
import org.sutormin.nanocraft.networking.coders.VarCoder;

public class Pong {
    public static short ID = 5;
    public static void make(ByteBuf buf, int id) {
        ByteBuf packet = Unpooled.buffer();
        VarCoder.writeVarInt(packet,id);
        PacketIO.write(buf, ID, packet);
        packet.release();
    }
}
