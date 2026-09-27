package org.sutormin.nanocraft.networking.packets.play;

import io.netty.buffer.ByteBuf;
import org.sutormin.nanocraft.networking.coders.VarCoder;
import org.sutormin.nanocraft.networking.packets.types.S2CPacket;
import org.sutormin.nanocraft.world.biome.BiomeTint;

/**
 * Join game ("login" in play) and respawn packets: only the hashed seed is used, for biome lookups.
 * Both end in the same spawn info (dimension type, dimension name, hashed seed, ...); the join
 * packet has its own fields first.
 */
public class S2CJoinGame implements S2CPacket {
    private final boolean respawn;

    public S2CJoinGame(boolean respawn) {
        this.respawn = respawn;
    }

    @Override
    public void read(ByteBuf buf) {
        if (!respawn) {
            buf.readInt();                                    // player entity id
            buf.readBoolean();                                // hardcore
            int levels = VarCoder.readVarInt(buf);            // dimension names
            for (int i = 0; i < levels; i++) VarCoder.readString(buf);
            VarCoder.readVarInt(buf);                         // max players
            VarCoder.readVarInt(buf);                         // view distance
            VarCoder.readVarInt(buf);                         // simulation distance
            buf.readBoolean();                                // reduced debug info
            buf.readBoolean();                                // show death screen
            buf.readBoolean();                                // limited crafting
        }
        VarCoder.readVarInt(buf);                             // dimension type
        VarCoder.readString(buf);                             // dimension name
        BiomeTint.setZoomSeed(buf.readLong());                // hashed seed
    }
}
