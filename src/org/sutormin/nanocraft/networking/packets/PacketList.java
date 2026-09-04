package org.sutormin.nanocraft.networking.packets;

import org.sutormin.nanocraft.networking.NetworkPhase;
import org.sutormin.nanocraft.networking.packets.s2c.EncryptionRequest;
import io.netty.channel.Channel;
import org.sutormin.nanocraft.networking.packets.s2c.SetCompression;

public class PacketList {
  public static S2CPacket getS2CPacket(
      int id,
      NetworkPhase phase,
      Channel channel
  ) {
    if (phase == NetworkPhase.LOGIN && id == 1) {
      return new EncryptionRequest(channel);
    }
    if (phase == NetworkPhase.LOGIN && id == 3) {
      return new SetCompression();
    }

    return null;
  }
}
