package com.malichenko.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.List;

public class Handler extends ChannelInboundHandlerAdapter {
    private enum Status {
        IDLE, FILE, COMMAND, LIST
    }

    private Status currentStatus;
    private ReceiverFile receiverFile;
    private ReceiverListFile receiverListFile;
    private ReceiverCommand receiverCommand;
    private Logger logger;


    private Runnable finishOperation = () -> {
        logger.debug("Операция завершена");
        currentStatus = Status.IDLE;
    };

    public Handler(Path userPath, ReceiverFile receiverFile, ReceiverCommand receiverCommand, ReceiverListFile receiverListFile) {
        this.currentStatus = Status.IDLE;
        this.receiverFile = receiverFile;
        this.receiverListFile = receiverListFile;
        this.receiverCommand = receiverCommand;
        this.logger = LogManager.getLogger();

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {
            if (currentStatus == Status.IDLE) {
                byte signalByte = buf.readByte();
                if (signalByte == ListSignalByte.FILE_SIGNAL_BYTE) {
                    logger.debug("channelRead signalByte = FILE_SIGNAL_BYTE signalByte:"+signalByte);
                    currentStatus = Status.FILE;
                    receiverFile.startReceive();
                } else if (signalByte == ListSignalByte.CMD_SIGNAL_BYTE) {
                    logger.debug("channelRead signalByte = CMD_SIGNAL_BYTE signalByte:"+signalByte);
                    currentStatus = Status.COMMAND;
                    receiverCommand.startReceive();
                } else if (signalByte == ListSignalByte.LIST_FILE_SIGNAL_BYTE) {
                    logger.debug("channelRead signalByte = LIST_FILE_SIGNAL_BYTE signalByte:"+signalByte);
                    currentStatus = Status.LIST;
                    receiverListFile.startReceive();
                }
            }
            if (currentStatus == Status.FILE) {
                logger.debug("channelRead Status.FILE");
                receiverFile.receive(ctx, buf, finishOperation);
            }
            if (currentStatus == Status.COMMAND) {
                logger.debug("channelRead Status.COMMAND");
                receiverCommand.receive(ctx, buf, finishOperation);
            }
            if (currentStatus == Status.LIST) {
                logger.debug("channelRead Status.LIST ");
                receiverListFile.receive(ctx, buf, finishOperation);
            }
        }
        if (buf.readableBytes() == 0) {
            buf.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        logger.error(cause);
        ctx.close();
    }
}
