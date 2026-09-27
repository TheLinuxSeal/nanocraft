package org.sutormin.nanocraft.networking.packets.login;

import io.netty.buffer.ByteBuf;
import org.sutormin.nanocraft.networking.Networking;
import org.sutormin.nanocraft.networking.coders.VarCoder;
import org.sutormin.nanocraft.networking.packets.types.S2CPacket;

import static org.sutormin.nanocraft.NanoCraft.NETWORKING;

public class S2CSetCompression implements S2CPacket {
    public void read(ByteBuf data) {
        NETWORKING.compressionThreshold = VarCoder.readVarInt(data);
    }
}
