package com.malichenko.yury.netty.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AuthSender extends ChannelOutboundHandlerAdapter{

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        String auth = msg.toString();

        ByteBuf buf = null;
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte((byte) 20); //сигнальный байт авторизации
        ctx.writeAndFlush(buf);
        System.out.println("send signal byte - 20");

        byte[] arr = auth.getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(arr.length); // длинна строки
        ctx.writeAndFlush(buf);
        System.out.println("send signal string length - " + arr.length);

        buf = ByteBufAllocator.DEFAULT.directBuffer(arr.length);
        buf.writeBytes(arr);
        ChannelFuture transferOperationFuture = ctx.writeAndFlush(buf);
        System.out.println("send signal string - " + auth);
    }

    public static void sendAuth(String auth, Channel channel, ChannelFutureListener finishListener) throws IOException {
        ByteBuf buf = null;
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte((byte) 20); //сигнальный байт авторизации
        channel.writeAndFlush(buf);
        System.out.println("send signal byte - 20");

        byte[] arr = auth.getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(arr.length); // длинна строки
        channel.writeAndFlush(buf);
        System.out.println("send signal string length - " + arr.length);

        buf = ByteBufAllocator.DEFAULT.directBuffer(arr.length);
        buf.writeBytes(arr);
        ChannelFuture transferOperationFuture = channel.writeAndFlush(buf);
        System.out.println("send signal string - " + auth);

        if (finishListener != null) { //добавить обработчик события отправки авторизации
            transferOperationFuture.addListener(finishListener);
            System.out.println("finishListener - " + transferOperationFuture);
        }
    }
}
