package com.malichenko.yury.netty.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class Sender {
    public static void sendFile(Path path, Channel channel, ChannelFutureListener finishListener) throws IOException {
        FileRegion region = new DefaultFileRegion(path.toFile(), 0, Files.size(path));
        byte[] filenameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        // 1 + 4 + filenameBytes.length + 8 -> SIGNAL_BYTE FILENAME_LENGTH(int) + FILENAME + FILE_LENGTH(long)
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + filenameBytes.length + 8);
        buf.writeByte(ListSignalByte.FILE_SIGNAL_BYTE);
        buf.writeInt(filenameBytes.length);
        buf.writeBytes(filenameBytes);
        buf.writeLong(Files.size(path));
        channel.writeAndFlush(buf);

        ChannelFuture transferOperationFuture = channel.writeAndFlush(region);
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }
    }

    public static void sendCommand(Channel channel, ListCommandByte command) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1+1);
        buf.writeByte(ListSignalByte.CMD_SIGNAL_BYTE);
        buf.writeByte(command.getByteValue());
        channel.writeAndFlush(buf);
    }

    public static void sendCommandAndFileName(Path path, Channel channel, ListCommandByte commandByte) {
        byte[] fileNameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1+1+4+fileNameBytes.length);
        buf.writeByte(ListSignalByte.CMD_SIGNAL_BYTE);
        buf.writeByte(commandByte.getByteValue());
        buf.writeInt(fileNameBytes.length);
        buf.writeBytes(fileNameBytes);
        channel.writeAndFlush(buf);
    }

    public static void sendFilesList(Path path, Channel channel, ListCommandByte command) {
        List<Path> list;
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(2097152); //объявим байт-буфер 2мб
        //TODO LocalDateTime lastModified;
        try {
            list = Files.list(path)
                    .filter(p -> !Files.isDirectory(p))
                    .collect(Collectors.toList());
            buf.writeByte(ListSignalByte.LIST_FILE_SIGNAL_BYTE);
            buf.writeByte(command.getByteValue());
            buf.writeInt(list.size());

        //TODO Из списка сначала в строку и потом в байты. написать стрим, который на выходе отдаст байт буфер//buf = list.stream().map(
            for (Path p : list) {
                //TODO lastModified = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(3));
                byte[] filenameBytes = p.getFileName().toString().getBytes(StandardCharsets.UTF_8);
                buf.writeInt(filenameBytes.length);
                buf.writeBytes(filenameBytes);
                buf.writeLong(Files.size(p));
                //TODO buf.writBytes(lastModified);
            }
            channel.writeAndFlush(buf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}