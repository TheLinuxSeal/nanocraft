package org.sutormin.nanocraft.networking;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class Networking {

  public static NetworkPhase networkPhase = NetworkPhase.HANDSHAKE;
  public static int compressionThreshold = -1;

  public static void init() {
    String host = "127.0.0.1";
    int port = 25565;

    EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

    try {
      Bootstrap bootstrap = new Bootstrap();
      bootstrap.group(group)
          .channel(NioSocketChannel.class)
          .handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
              // --- THE PIPELINE CONFIGURATION ---


              // 3. Initialize and add your custom handler
              ch.pipeline().addLast(new NettyClient());
            }
          });

      // Connect to the server
      ChannelFuture future = bootstrap.connect(host, port).sync();



      // Wait until the connection is closed
      future.channel().closeFuture().sync();

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      e.printStackTrace();
    } finally {
      group.shutdownGracefully();
    }
  }
}