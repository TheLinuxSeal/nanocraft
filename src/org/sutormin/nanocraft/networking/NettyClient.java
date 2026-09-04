package org.sutormin.nanocraft.networking;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.sutormin.nanocraft.NanoCraft;
import org.sutormin.nanocraft.Options;
import org.sutormin.nanocraft.networking.coders.PacketIO;
import org.sutormin.nanocraft.networking.coders.PacketIOer;
import org.sutormin.nanocraft.networking.packets.c2s.Handshake;
import org.sutormin.nanocraft.networking.packets.c2s.LoginStart;


public class NettyClient extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ByteBuf packet = Unpooled.buffer();
        Handshake.make(packet, "127.0.0.1", 25565);
        ctx.write(packet);

        ByteBuf packet2 = Unpooled.buffer();
        LoginStart.make(packet2, Options.PLAYER_USERNAME, Options.PLAYER_UUID);
        ctx.writeAndFlush(packet2);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ByteBuf in) {
            try {
                /*System.out.println("[Client] Server finally sent data back!");
                System.out.println("Readable payload bytes: " + in.readableBytes());

                int length = VarCoder.readVarInt(in);
                int packetId = VarCoder.readVarInt(in);

                System.out.printf("Packet Received -> Length: %d, ID: 0x%02X\n", length, packetId);

                if (packetId == 0x00) { // Disconnect Packet
                    // Read the custom Minecraft string format (VarInt length + UTF-8 bytes)
                    int reasonLength = VarCoder.readVarInt(in);
                    byte[] reasonBytes = new byte[reasonLength];
                    in.readBytes(reasonBytes);
                    String jsonReason = new String(reasonBytes, java.nio.charset.StandardCharsets.UTF_8);

                    System.out.println("\n❌ DISCONNECTED BY SERVER! Reason:");
                    System.out.println(jsonReason);
                    System.out.println();
                }*/
                PacketIO.read(in,ctx.channel());

            } finally {
                in.release();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("[Client] Connection dropped or encountered an error:");
        cause.printStackTrace();
        ctx.close();
    }
}