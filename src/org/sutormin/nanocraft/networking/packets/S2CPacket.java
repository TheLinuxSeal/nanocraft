package org.sutormin.nanocraft.networking.packets;

import io.netty.buffer.ByteBuf;

public interface S2CPacket {
    void read(ByteBuf buf);
}
