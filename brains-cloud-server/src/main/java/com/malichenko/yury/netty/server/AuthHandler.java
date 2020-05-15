package com.malichenko.yury.netty.server;

import com.malichenko.yury.netty.common.FileGetter;
import com.malichenko.yury.netty.common.FileSender;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class AuthHandler extends ChannelInboundHandlerAdapter {
    public enum State {
        IDLE, STRING_LENGTH, COMMAND, COMMAND_AUTH
    }

    private String username;
    private String commandAuth;
    private AuthHandler.State currentState = AuthHandler.State.IDLE;
    private int stringLength;
    private boolean authOk = false;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (authOk) {
            ctx.fireChannelRead(msg);
            return;
        }
        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {
            if (currentState == AuthHandler.State.IDLE) {
                byte readed = buf.readByte();
                if (readed == (byte) 20) {
                    currentState = AuthHandler.State.STRING_LENGTH;
                    System.out.println("STATE: Start auth");
                } else {
                    System.out.println("ERROR: Invalid auth, first byte - " + readed);
                }
            }
            if (currentState == AuthHandler.State.STRING_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    stringLength = buf.readInt();
                    System.out.println("STATE: Get string command length ok - " + stringLength);
                    currentState = AuthHandler.State.COMMAND;
                }
            }
            if (currentState == AuthHandler.State.COMMAND) {
                if (buf.readableBytes() >= stringLength) {
                    byte[] fileName = new byte[stringLength];
                    buf.readBytes(fileName);
                    commandAuth = new String(fileName, "UTF-8");
                    System.out.println("STATE: command received - " + commandAuth);
                    currentState = AuthHandler.State.COMMAND_AUTH;
                }
            }
            if (currentState == AuthHandler.State.COMMAND_AUTH) {
                if (commandAuth.split(" ")[0].equals("/auth")) {
                    username = commandAuth.split(" ")[1];
                    authOk = true;
                    System.out.println("STATE: Auth ok - " + username);
                    currentState = AuthHandler.State.IDLE;
                    ctx.pipeline().addLast(new FileSender());
                    ctx.pipeline().addLast(new FileGetter(username));
                } else {
                    System.out.println("ERROR: Invalid auth, /auth " + username);
                    currentState = AuthHandler.State.IDLE;
                }
                break;
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
