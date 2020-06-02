package com.malichenko.yury.netty.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public abstract class ReceiverCommand {
    public enum State {
        IDLE, COMMAND_LENGTH, COMMAND
    }

    private State currentState = State.IDLE;
    private int commandTypeLength;
    private byte command;
    private byte[] cmd;

    public void startReceive() {
        currentState = State.COMMAND_LENGTH;
    }

    public void receive(ChannelHandlerContext ctx, ByteBuf buf, Runnable finishOperation) throws Exception {
        if (currentState == State.COMMAND_LENGTH) {
            if (buf.readableBytes() >= 4) {
                commandTypeLength = buf.readInt();
                currentState = State.COMMAND;
            }
        }
        if (currentState == State.COMMAND) {
            command = buf.readByte();
            if (commandTypeLength != 1) {
                cmd = new byte[commandTypeLength - 1];
                buf.readBytes(cmd);
            } else {
                cmd = new byte[0];
            }
            parseCommand(ctx, command, cmd);
            currentState = State.IDLE;
            finishOperation.run();
        }
    }

    public abstract void parseCommand(ChannelHandlerContext ctx, byte command, byte[] fileName) throws Exception;
}
