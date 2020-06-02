package com.malichenko.yury.netty.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public abstract class ReceiverListFile {

    public abstract void startReceive();

    public abstract void receive(ChannelHandlerContext ctx, ByteBuf buf, Runnable finishOperation) throws Exception;
}