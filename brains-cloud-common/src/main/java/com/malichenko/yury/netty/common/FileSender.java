package com.malichenko.yury.netty.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileSender extends ChannelOutboundHandlerAdapter{

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Path path = Paths.get(msg.toString());
        FileRegion region = new DefaultFileRegion(path.toFile(), 0, Files.size(path));

        System.out.println("Start file sending:");
        ByteBuf buf = null;
        buf = ByteBufAllocator.DEFAULT.directBuffer(1); //size buf
        buf.writeByte((byte) 25);
        ctx.writeAndFlush(buf); //send signal byte 25
        System.out.println("send signal byte - 25");


        byte[] filenameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(filenameBytes.length);
        ctx.writeAndFlush(buf); //send fileLength
        System.out.println("send fileLength - " + filenameBytes.length);

        buf = ByteBufAllocator.DEFAULT.directBuffer(filenameBytes.length);
        buf.writeBytes(filenameBytes);
        ctx.writeAndFlush(buf); //send fileName
        System.out.println("send fileName - " + filenameBytes);

        buf = ByteBufAllocator.DEFAULT.directBuffer(8);
        buf.writeLong(Files.size(path));
        ctx.writeAndFlush(buf); //send fileSize
        System.out.println("send fileSize - " + Files.size(path));

        ctx.writeAndFlush(region); //send fileData ZeroCopy
        System.out.println("send fileData");
    }

    public static void sendFile(Path path, Channel channel, ChannelFutureListener finishListener) throws IOException {
        FileRegion region = new DefaultFileRegion(path.toFile(), 0, Files.size(path));

        System.out.println("Start file sending:");
        ByteBuf buf = null;
        buf = ByteBufAllocator.DEFAULT.directBuffer(1); //size buf
        buf.writeByte((byte) 25);
        channel.writeAndFlush(buf); //send signal byte 25
        System.out.println("send signal byte - 25");


        byte[] filenameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(filenameBytes.length);
        channel.writeAndFlush(buf); //send fileLength
        System.out.println("send fileLength - " + filenameBytes.length);

        buf = ByteBufAllocator.DEFAULT.directBuffer(filenameBytes.length);
        buf.writeBytes(filenameBytes);
        channel.writeAndFlush(buf); //send fileName
        System.out.println("send fileName - " + filenameBytes);

        buf = ByteBufAllocator.DEFAULT.directBuffer(8);
        buf.writeLong(Files.size(path));
        channel.writeAndFlush(buf); //send fileSize
        System.out.println("send fileSize - " + Files.size(path));

        ChannelFuture transferOperationFuture = channel.writeAndFlush(region); //send fileData ZeroCopy
        System.out.println("send fileData");

        if (finishListener != null) { //добавить обработчик события отправки файла висит в клиенте
            transferOperationFuture.addListener(finishListener);
            System.out.println("finishListener - " + transferOperationFuture);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}