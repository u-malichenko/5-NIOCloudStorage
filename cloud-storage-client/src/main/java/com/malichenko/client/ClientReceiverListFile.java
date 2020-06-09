package com.malichenko.client;

import com.malichenko.common.FileInfo;
import com.malichenko.common.ReceiverListFile;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClientReceiverListFile extends ReceiverListFile {
    public enum State {
        IDLE, COMMAND_LENGTH, COMMAND
    }

    private long fileSize;
    private int commandLength;
    private int receivedLength;
    private ByteBuf buffer;
    private StringBuilder cmd;
    private String fileName;
    private String userDirectory;
    private Path userPath;
    private Path filePath;
    private State currentState;
    private List<Callback> callbackList;
    private Logger logger;


    public ClientReceiverListFile(Path userPath, List<Callback> callbackList) {
        this.currentState = State.IDLE;
        this.userPath = userPath;
        this.callbackList = callbackList;
        this.userDirectory = GUIHelper.userDirectory;
        this.logger = LogManager.getLogger();
    }

    @Override
    public void startReceive() {
        if (currentState == State.IDLE) {
            GUIHelper.serverFilesList.clear();
            currentState = State.COMMAND_LENGTH;
            cmd = new StringBuilder();
            buffer = ByteBufAllocator.DEFAULT.directBuffer(65536);
            logger.debug("startReceive начало передачи листа файлов");
        }else{
            logger.debug("startReceive state.ERRoR");
        }
    }

    @Override
    public void receive(ChannelHandlerContext ctx, ByteBuf buf, Runnable finishOperation) {
        if (currentState == State.COMMAND_LENGTH) {
            if (buf.readableBytes() >= 4) {
                commandLength = buf.readInt();
                logger.debug("receive Получена длина команды " + commandLength);
                if (commandLength > buffer.capacity()) {
                    buffer.release();
                    buffer = ByteBufAllocator.DEFAULT.directBuffer(commandLength);
                    logger.debug("receive длина команды больше размера буфера создаем новый буфер");
                }
                currentState = State.COMMAND;
                receivedLength = 0;
                cmd.setLength(0);
            }
        }
        if (currentState == State.COMMAND) {
            while (buf.readableBytes() > 0) {
                buffer.writeByte(buf.readByte());
                receivedLength++;
                if (receivedLength == commandLength) {
                    parseCommand(ctx, buffer.toString(StandardCharsets.UTF_8));
                    buffer.clear();
                    currentState = State.IDLE;
                    finishOperation.run();
                    callbackList.get(1).callback();
                    logger.debug("List file received ok (callbackList.get(1).callback())");
                    return;
                }
            }
        }
    }

    public void parseCommand(ChannelHandlerContext ctx, String cmd) {
        List<String> list = Arrays.stream(cmd.split("[|]")).collect(Collectors.toList());
        for(String s : list){
            logger.debug("parseCommand строка: " + s);
            fileName = s.split("[*]")[0];
            logger.debug("parseCommand fileName: " + fileName);
            fileSize = Long.parseLong(s.split("[*]")[1]);
            logger.debug("parseCommand fileSize: " + fileSize);
            filePath = Paths.get("server_storage", userDirectory, fileName);
            logger.debug("parseCommand filePath: " + filePath);
            FileInfo fileInfo = new FileInfo(filePath.toFile(), fileName, fileSize);
            GUIHelper.serverFilesList.add(fileInfo);
        }
        logger.debug("parseCommand UIHelper.serverFilesList.add(fileInfo) - ok");
    }
}
