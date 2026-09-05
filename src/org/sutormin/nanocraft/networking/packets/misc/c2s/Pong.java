package org.sutormin.nanocraft.networking.packets.misc.c2s;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.sutormin.nanocraft.networking.NetworkPhase;
import org.sutormin.nanocraft.networking.Networking;
import org.sutormin.nanocraft.networking.coders.PacketIO;
import org.sutormin.nanocraft.networking.coders.VarCoder;

public class Pong {
    public static short CONFIG_ID = 5;
    public static short PLAY_ID = 45;
    public static void make(ByteBuf buf, int id) {
        ByteBuf packet = Unpooled.buffer();
        VarCoder.writeVarInt(packet,id);
        if (Networking.networkPhase == NetworkPhase.CONFIG) PacketIO.write(buf, CONFIG_ID, packet);
        if (Networking.networkPhase == NetworkPhase.PLAY) PacketIO.write(buf, PLAY_ID, packet);
        packet.release();
    }
}
