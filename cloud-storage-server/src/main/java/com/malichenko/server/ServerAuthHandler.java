package com.malichenko.server;

import com.malichenko.common.ListCommandByte;
import com.malichenko.common.Handler;
import com.malichenko.common.ListSignalByte;
import com.malichenko.common.Sender;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * первичный обработчик входящих данных осуществляет авторизацию клиента с запросом к БД
 */
public class ServerAuthHandler extends ChannelInboundHandlerAdapter {
    private ServerDBAuthService authService;

    public enum State {
        IDLE, LOGIN_LENGTH, LOGIN, PASSWORD_LENGTH, PASSWORD
    }

    private int loginLength;
    private int passwordLength;
    private String login;
    private String password;
    private State currentState;
    private Logger logger;
    private Path userPath;

    public ServerAuthHandler(ServerDBAuthService authService) {
        this.authService = authService;
        this.logger = LogManager.getLogger();
        this.currentState = State.IDLE;
    }

    /**
     * метод ЧТЕНИЯ данных из канала
     */
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = ((ByteBuf) msg);

        try {
            getLoginAndPassword(buf);
        } catch (UnsupportedEncodingException e) {
            logger.error(e);
        }

        String userDirectory = authService.getDirectoryByLoginPass(login, password);

        if (userDirectory == null) {
            Sender.sendCommand(ctx.channel(), ListCommandByte.AUTH_ERR);
            logger.debug("Неверный логин: " + login + " или пароль: " + password);
            return;
        } else {
            if (!Files.exists(Paths.get("server_storage", userDirectory))) {
                try {
                    Files.createDirectory(Paths.get("server_storage", userDirectory));
                } catch (IOException e) {
                    logger.error(e);
                    e.printStackTrace();
                }
            }
            Sender.sendCommand(ctx.channel(), ListCommandByte.AUTH_OK);
            userPath = Paths.get("server_storage", userDirectory);
            ctx.pipeline().addLast(new Handler(userPath,
                    new ServerReceiverFile(userPath),
                    new ServerReceiverCommand(userPath),
                    new ServerReceiverListFile()));
            ctx.pipeline().remove(this);
            logger.debug("Пользователь : " + login + " успешно прошел авторизацию.");
        }
    }

    /**
     * метод получения лигина и пароля из потока
     */
    private void getLoginAndPassword(ByteBuf buf) throws UnsupportedEncodingException {

        while (buf.readableBytes() > 0) {
            if (currentState == State.IDLE) {
                byte controlByte = buf.readByte();
                if (controlByte == ListSignalByte.CMD_SIGNAL_BYTE) {
                    logger.debug("STATE: Get controlByte - " + controlByte);
                    currentState = State.LOGIN_LENGTH;
                }else {
                    logger.debug("STATE - IDLE Get NOT controlByte: " + controlByte);
                    break;
                }
            }
            if (currentState == State.LOGIN_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    loginLength = buf.readInt();
                    logger.debug("STATE - LOGIN_LENGTH: " + loginLength);
                    currentState = State.LOGIN;
                }
            }
            if (currentState == State.LOGIN) {
                if (buf.readableBytes() >= loginLength) {
                    byte[] loginBytes = new byte[loginLength];
                    buf.readBytes(loginBytes);
                    login = new String(loginBytes, "UTF-8");
                    logger.debug("STATE - LOGIN: " + login);
                    currentState = State.PASSWORD_LENGTH;
                }
            }
            if (currentState == State.PASSWORD_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    passwordLength = buf.readInt();
                    logger.debug("STATE - PASSWORD_LENGTH: " + passwordLength);
                    currentState = State.PASSWORD;
                }
            }
            if (currentState == State.PASSWORD) {
                if (buf.readableBytes() >= passwordLength) {
                    byte[] passwordBytes = new byte[passwordLength];
                    buf.readBytes(passwordBytes);
                    password = new String(passwordBytes, "UTF-8");
                    logger.debug("STATE - PASSWORD: " + password);
                }
                currentState = State.IDLE;
                break;
            }
        }
        if (buf.readableBytes() == 0) {
            buf.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        logger.error(cause);
        ctx.close();
    }
}
