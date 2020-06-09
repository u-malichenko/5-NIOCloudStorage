package com.malichenko.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class Sender {
    private static Logger logger = LogManager.getLogger();

    public static void sendFile(Path path, Channel channel, ChannelFutureListener finishListener) throws IOException {
        FileRegion region = new DefaultFileRegion(path.toFile(), 0, Files.size(path));
        byte[] filenameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        // 1 + 4 + filenameBytes.length + 8 -> SIGNAL_BYTE FILENAME_LENGTH(int) + FILENAME + FILE_LENGTH(long)
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + filenameBytes.length + 8);
        buf.writeByte(ListSignalByte.FILE_SIGNAL_BYTE);
        logger.debug("sendFile 1 writeByte SignalByte: " + ListSignalByte.FILE_SIGNAL_BYTE);
        buf.writeInt(filenameBytes.length);
        logger.debug("sendFile 4 writeInt filenameBytes.length: " + filenameBytes.length);
        buf.writeBytes(filenameBytes);
        logger.debug("sendFile writeBytes filenameBytes: " + filenameBytes);
        buf.writeLong(Files.size(path));
        logger.debug("sendFile 4 writeLong size: " + Files.size(path));
        channel.writeAndFlush(buf);
        ChannelFuture transferOperationFuture = channel.writeAndFlush(region);
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }
    }

    public static void sendCommand(Channel channel, ListCommandByte command) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 1);
        buf.writeByte(ListSignalByte.CMD_SIGNAL_BYTE);
        logger.debug("sendCommand 1 writeByte SignalByte: " + ListSignalByte.CMD_SIGNAL_BYTE);
        buf.writeByte(command.getByteValue());
        logger.debug("sendCommand 1 writeByte command: " + command.getByteValue());
        channel.writeAndFlush(buf);
    }

    public static void sendCommandAndFileName(Path path, Channel channel, ListCommandByte commandByte) {
        byte[] fileNameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        logger.debug("sendCommandAndFileName fileNameBytes: " + path.getFileName().toString());
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 1 + 4 + fileNameBytes.length);
        buf.writeByte(ListSignalByte.CMD_SIGNAL_BYTE);
        logger.debug("sendCommandAndFileName 1 writeByte SignalByte: " + ListSignalByte.CMD_SIGNAL_BYTE);
        buf.writeByte(commandByte.getByteValue());
        logger.debug("sendCommandAndFileName 1 writeByte commandByte: " + commandByte.getByteValue());
        buf.writeInt(fileNameBytes.length);
        logger.debug("sendCommandAndFileName 4 writeInt fileNameBytes.length: " + fileNameBytes.length);
        buf.writeBytes(fileNameBytes);
        logger.debug("sendCommandAndFileName writeBytes fileNameBytes: " + path.getFileName().toString());
        channel.writeAndFlush(buf);
    }

    public static void sendFilesList(Path path, Channel channel, ListCommandByte command) {
        try {
            String filesList = Files.list(path)
                    .filter(p -> !Files.isDirectory(p))
                    .map(FileInfo::new)
                    //.sorted(Comparator.comparing(FileInfo::getName))
                    .map(FileInfo::getInfo)
                    .collect(Collectors.joining("|"));
            logger.debug("sendFilesList list: " + filesList);
            byte[] stringBytes = filesList.getBytes(StandardCharsets.UTF_8);
            logger.debug("sendFilesList создали массив байт строки : " + filesList);
            ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + stringBytes.length);
            buf.writeByte(ListSignalByte.LIST_FILE_SIGNAL_BYTE);
            logger.debug("sendFilesList добавили в буфер сигнальный байт LIST_FILE_SIGNAL_BYTE : " + ListSignalByte.LIST_FILE_SIGNAL_BYTE);
            buf.writeInt(stringBytes.length);
            logger.debug("sendFilesList добавили в буфер длину строки : " + stringBytes.length);
            buf.writeBytes(stringBytes);
            logger.debug("sendFilesList добавили в буфер байты строки: " + filesList);
            channel.writeAndFlush(buf);
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e);
        }
    }
}