package com.malichenko.yury.netty.server;

import com.malichenko.yury.netty.common.ReceiverListFile;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class ServerReceiverListFile extends ReceiverListFile {
    @Override
    public void startReceive() {

    }

    @Override
    public void receive(ChannelHandlerContext ctx, ByteBuf buf, Runnable finishOperation) throws Exception {

    }
}
