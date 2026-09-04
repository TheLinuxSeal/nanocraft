package org.sutormin.nanocraft.networking.coders;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.sutormin.nanocraft.networking.Networking;

public class PacketIO {
    public static void read(ByteBuf buf, Channel channel){
        if (Networking.compressionThreshold >= 0) {
            PacketIOer.readCompressedPacket(buf, channel);
        } else {
            PacketIOer.readPacket(buf, channel);
        }
    }
    
    public static void write(ByteBuf out, int packetId, ByteBuf data){
        if (Networking.compressionThreshold >= 0) {
            PacketIOer.writeCompressedPacket(
                out,
                packetId,
                data,
                Networking.compressionThreshold
            );
        } else {
            PacketIOer.writePacket(out, packetId, data);
        }
    }
}
