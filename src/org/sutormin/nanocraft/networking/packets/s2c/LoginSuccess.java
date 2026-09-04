package org.sutormin.nanocraft.networking.packets.s2c;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.sutormin.nanocraft.networking.packets.S2CPacket;
import org.sutormin.nanocraft.networking.packets.c2s.LoginAcknowledged;

public class LoginSuccess implements S2CPacket {
  private final Channel channel;

  public LoginSuccess(Channel channel) {
    this.channel = channel;
  }
  public void read(ByteBuf data) {
    ByteBuf out = channel.alloc().buffer();
    LoginAcknowledged.make(
        out
    );
    channel.writeAndFlush(out);
  }
}

