package com.malichenko.yury.netty.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.file.Path;

public class Handler extends ChannelInboundHandlerAdapter {
    private enum Status {
        IDLE, FILE, COMMAND, LIST
    }

    private Status currentStatus;
    private ReceiverFile receiverFile;
    private ReceiverListFile receiverListFile;
    private ReceiverCommand receiverCommand;


    private Runnable finishOperation = () -> {
        System.out.println("Операция завершена");
        //todo обновить список файлов callbackList.get(2).callback(); 1
        currentStatus = Status.IDLE;
    };

    public Handler(Path userPath, ReceiverCommand receiverCommand, ReceiverListFile receiverListFile) {
        this.currentStatus = Status.IDLE;
        this.receiverFile = new ReceiverFile(userPath);
        this.receiverListFile = receiverListFile;
        this.receiverCommand = receiverCommand;

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {
            if (currentStatus == Status.IDLE) {
                byte signalByte = buf.readByte();
                if (signalByte == ListSignalByte.FILE_SIGNAL_BYTE) {
                    currentStatus = Status.FILE;
                    receiverFile.startReceive();
                } else if (signalByte == ListSignalByte.CMD_SIGNAL_BYTE) {
                    currentStatus = Status.COMMAND;
                    receiverCommand.startReceive();
                } else if (signalByte == ListSignalByte.LIST_FILE_SIGNAL_BYTE) {
                    currentStatus = Status.LIST;
                    receiverListFile.startReceive();
                }
            }
            if (currentStatus == Status.FILE) {
                receiverFile.receive(ctx, buf, finishOperation);
            }
            if (currentStatus == Status.COMMAND) {
                receiverCommand.receive(ctx, buf, finishOperation);
            }
            if (currentStatus == Status.LIST) {
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
        ctx.close();
    }
}
