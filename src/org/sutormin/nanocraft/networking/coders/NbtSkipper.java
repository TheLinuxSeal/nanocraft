package org.sutormin.nanocraft.networking.coders;

import io.netty.buffer.ByteBuf;

/** Reads past network NBT (a nameless root tag, as sent since 1.20.2) without decoding it. */
public final class NbtSkipper {
    private NbtSkipper() {}

    public static void skipRoot(ByteBuf buf) {
        int type = buf.readUnsignedByte();
        if (type != 0) skipPayload(buf, type);
    }

    private static void skipPayload(ByteBuf buf, int type) {
        switch (type) {
            case 1 -> buf.skipBytes(1);                          // byte
            case 2 -> buf.skipBytes(2);                          // short
            case 3, 5 -> buf.skipBytes(4);                       // int, float
            case 4, 6 -> buf.skipBytes(8);                       // long, double
            case 7 -> buf.skipBytes(buf.readInt());              // byte array
            case 8 -> buf.skipBytes(buf.readUnsignedShort());    // string
            case 9 -> {                                          // list
                int elementType = buf.readUnsignedByte();
                int length = buf.readInt();
                for (int i = 0; i < length; i++) skipPayload(buf, elementType);
            }
            case 10 -> {                                         // compound
                int child;
                while ((child = buf.readUnsignedByte()) != 0) {
                    buf.skipBytes(buf.readUnsignedShort());      // name
                    skipPayload(buf, child);
                }
            }
            case 11 -> buf.skipBytes(buf.readInt() * 4);         // int array
            case 12 -> buf.skipBytes(buf.readInt() * 8);         // long array
            default -> throw new IllegalStateException("Unknown NBT tag type " + type);
        }
    }
}
