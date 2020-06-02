package com.malichenko.yury.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class ServerApp {
    private static final int PORT = 8189;
    private ServerDBAuthService authService;

    public void run() throws Exception {
        authService = new ServerDBAuthService();
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new ServerAuthHandler(authService));
                        }
                    });
            ChannelFuture f = b.bind(PORT).sync();
            System.out.println("Сервер запущен на порту: " + PORT);
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            authService.disconnect();
        }
    }

    public static void main(String[] args) throws Exception {
        new ServerApp().run();
    }
}