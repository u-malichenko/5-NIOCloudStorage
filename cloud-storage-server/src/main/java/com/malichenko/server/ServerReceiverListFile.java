package com.malichenko.server;

import com.malichenko.common.ReceiverListFile;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class ServerReceiverListFile extends ReceiverListFile {
    @Override
    public void startReceive() {
        System.out.println("ServerReceiverListFile startReceive ?,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,");
    }

    @Override
    public void receive(ChannelHandlerContext ctx, ByteBuf buf, Runnable finishOperation) throws Exception {
        System.out.println("ServerReceiverListFile receive ?,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,");
    }
}
