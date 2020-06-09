package com.malichenko.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class ReceiverCommand {
    public enum State {
        IDLE, COMMAND, COMMAND_BODY_LENGTH, BODY_COMMAND, TO_PARSE
    }

    private Logger logger= LogManager.getLogger();
    private State currentState = State.IDLE;
    private int commandTypeLength;
    private byte command;
    private byte[] cmd;

    public void startReceive() {
        currentState = State.COMMAND;
        logger.debug("ReceiverCommand startReceive State.COMMAND");
    }

    public void receive(ChannelHandlerContext ctx, ByteBuf buf, Runnable finishOperation) throws Exception {
        if (currentState == State.COMMAND) {
            command = buf.readByte();
            if(buf.readableBytes() >= 4) {
                logger.debug("ReceiverCommand receive State.COMMAND - 1COMMAND_BODY_LENGTH command: "+command);
                currentState = State.COMMAND_BODY_LENGTH;
            }else {
                cmd = null; //команда без тела
                logger.debug("ReceiverCommand receive State.COMMAND команда без тела - TO_PARSE: "+command);
                currentState = State.TO_PARSE;
            }
        }
        if (currentState == State.COMMAND_BODY_LENGTH) {
            if (buf.readableBytes() >= 4) {
                commandTypeLength = buf.readInt();
                logger.debug("ReceiverCommand receive State.COMMAND_BODY_LENGTH :" + commandTypeLength);
                currentState = State.BODY_COMMAND;
            }
        }
        if (currentState == State.BODY_COMMAND) {
            if (buf.readableBytes() >= commandTypeLength) {
                cmd = new byte[commandTypeLength];
                buf.readBytes(cmd);
                logger.debug("ReceiverCommand receive State.BODY_COMMAND cmd:" + cmd.toString());
                currentState = State.TO_PARSE;
            }
        } if (currentState == State.TO_PARSE) {
            parseCommand(ctx, command, cmd);
            logger.debug("ReceiverCommand receive State.TO_PARSE END STATE = IDLE");
            currentState = State.IDLE;
            finishOperation.run();
        }
    }

    public abstract void parseCommand(ChannelHandlerContext ctx, byte command, byte[] fileName) throws Exception;
}
