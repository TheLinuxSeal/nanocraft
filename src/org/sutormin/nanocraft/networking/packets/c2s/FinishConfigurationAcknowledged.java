package org.sutormin.nanocraft.networking.packets.c2s;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.sutormin.nanocraft.networking.coders.PacketIO;

public class FinishConfigurationAcknowledged {
    public static int ID = 3;
    public static void make(ByteBuf buf){
        ByteBuf packet = Unpooled.buffer();
        PacketIO.write(buf,ID,packet);
        packet.release();
    }
}
