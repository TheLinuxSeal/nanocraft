package org.sutormin.nanocraft.networking.packets.c2s;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.sutormin.nanocraft.NanoCraft;
import org.sutormin.nanocraft.networking.NetworkPhase;
import org.sutormin.nanocraft.networking.coders.PacketIO;
import org.sutormin.nanocraft.networking.coders.PacketIOer;
import org.sutormin.nanocraft.networking.coders.VarCoder;
import org.sutormin.nanocraft.networking.packets.C2SPacket;

import java.util.UUID;

public class LoginStart implements C2SPacket {
  public static short ID = 0;
  public static void make(ByteBuf buf, String username, UUID uuid){
    NanoCraft.networkPhase = NetworkPhase.LOGIN;
    ByteBuf packet = Unpooled.buffer();
    VarCoder.writeString(packet,username);
    packet.writeLong(uuid.getMostSignificantBits());
    packet.writeLong(uuid.getLeastSignificantBits());
    PacketIO.write(buf,ID,packet);
    packet.release();
  }
}
