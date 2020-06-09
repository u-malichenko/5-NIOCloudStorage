package com.malichenko.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;

public abstract class ReceiverFile {
    public enum State {
        IDLE, NAME_LENGTH, NAME, FILE_LENGTH, FILE
    }

    private State currentState = State.IDLE;
    private int nextLength;
    private long fileLength;
    private long receivedFileLength;
    private BufferedOutputStream out;
    private Path userPath;
    private Path filePath;
    private Logger logger;

    public ReceiverFile(Path userPath) {
        this.userPath = userPath;
        logger = LogManager.getLogger();
    }

    public void startReceive() {
        currentState = State.NAME_LENGTH;
        receivedFileLength = 0L;
        logger.debug("start State.NAME_LENGTH Receive Start file receiving");
    }

    public void receive(ChannelHandlerContext ctx, ByteBuf buf, Runnable finishOperation) throws Exception {
        if (currentState == State.NAME_LENGTH) {
            if (buf.readableBytes() >= 4) {
                nextLength = buf.readInt();
                logger.debug("receive State.NAME_LENGTH Get filename length nextLength: " + nextLength);
                currentState = State.NAME;
            }
        }
        if (currentState == State.NAME) {
            if (buf.readableBytes() >= nextLength) {
                byte[] fileName = new byte[nextLength];
                buf.readBytes(fileName);
                logger.debug("receive State.NAME Filename received - " + new String(fileName, "UTF-8"));
                getFilePath(userPath, fileName);
                out = new BufferedOutputStream(new FileOutputStream(String.valueOf(filePath)));
                currentState = State.FILE_LENGTH;
            }
        }
        if (currentState == State.FILE_LENGTH) {
            if (buf.readableBytes() >= 8) {
                fileLength = buf.readLong();
                logger.debug("receive State.FILE_LENGTH File length received - " + fileLength);
                currentState = State.FILE;
            }
        }
        if (currentState == State.FILE) {
            if (fileLength == receivedFileLength) {
                currentState = State.IDLE;
                logger.debug("receive State.FILE File received");
                out.close();
                callback(ctx);
                //todo callback обновить список файлов

                finishOperation.run();
                return;
            }
            while (buf.readableBytes() > 0) {
                out.write(buf.readByte());
                receivedFileLength++;
                if (fileLength == receivedFileLength) {
                    currentState = State.IDLE;
                    logger.debug("receive State.FILE File received");
                    out.close();
                    callback(ctx);
                    finishOperation.run();
                    return;
                }
            }
        }
    }
    public abstract void callback(ChannelHandlerContext ctx);

    /**
     * метод получения полного пути(с именем файла) папка пользователя + имя файла
     */
    private void getFilePath(Path userPath, byte[] fileName) {
        try {
            filePath = userPath.resolve(new String(fileName, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            logger.error(e);
        }
    }
}
