package org.sutormin.nanocraft.networking.packets;

import org.sutormin.nanocraft.networking.NetworkPhase;
import org.sutormin.nanocraft.networking.packets.s2c.*;
import io.netty.channel.Channel;

public class PacketList {
  public static S2CPacket getS2CPacket(int id, NetworkPhase phase, Channel channel) {
    if (phase == NetworkPhase.LOGIN && id == 1) return new EncryptionRequest(channel);
    if (phase == NetworkPhase.LOGIN && id == 3) return new SetCompression();
    if (phase == NetworkPhase.LOGIN && id == 2) return new LoginSuccess(channel);
    if (phase == NetworkPhase.CONFIG && id == 1) return new IgnorePacket("PluginMessage");
    if (phase == NetworkPhase.CONFIG && id == 12) return new IgnorePacket("FeatureFlags");
    if (phase == NetworkPhase.CONFIG && id == 14) return new KnownPacksChallenge(channel);
    if (phase == NetworkPhase.CONFIG && id == 7) return new IgnorePacket("RegistryData");
    if (phase == NetworkPhase.CONFIG && id == 13) return new IgnorePacket("UpdateTags");
    if (phase == NetworkPhase.CONFIG && id == 3) return new FinishConfiguration(channel);

    return null;
  }
}
