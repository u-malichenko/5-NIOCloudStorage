package com.malichenko.client;

import com.malichenko.common.ListCommandByte;
import com.malichenko.common.ListSignalByte;
import com.malichenko.common.Handler;
import com.malichenko.common.ReceiverFile;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * первичный обработчик входящих данных до авторизации
 */
public class ClientAuthHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = LogManager.getLogger();
    private Path userPath;
    private String userDirectory;
    private List<Callback> callbackList;

    public ClientAuthHandler(List<Callback> callbackList) {
        this.callbackList = callbackList;
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = ((ByteBuf) msg);
        byte controlByte = buf.readByte();
        logger.debug("channelRead buf.readByte() вычитали 1 сигнальный байт controlByte: " + controlByte);
        if (controlByte == ListSignalByte.CMD_SIGNAL_BYTE) {
            logger.debug("channelRead сигнальный байт = CMD_SIGNAL_BYTE: " + controlByte);
            byte cmd = buf.readByte();
            logger.debug("channelRead buf.readByte() вычитали 1 командный байт cmd: " + cmd);
            if (cmd == ListCommandByte.AUTH_OK.getByteValue()) {
                logger.debug("channelRead командный байт = AUTH_OK: " + cmd);
                userDirectory = GUIHelper.userDirectory; //вытаскиваем из временного хранилища переменную папки пользователя
                logger.debug("Директория пользователя: " + userDirectory);
                userPath = Paths.get("client_storage", userDirectory);
                logger.debug("Путь к папке пользователя: " + userPath); //todo из пути можно вытаскивать папку пользователя
                ctx.pipeline().addLast(new Handler(userPath,
                        new ClientReceiverFile(userPath, callbackList),
                        new ClientReceiverCommand(userPath, callbackList),
                        new ClientReceiverListFile(userPath, callbackList)));
                logger.debug("В конвеер добавили Handlerl и удалили ClientAuthHandler");
                ctx.pipeline().remove(this);
                logger.debug("Пользователь : " + userDirectory + " успешно прошел авторизацию.");
                logger.debug("Запускаем обновление интерфейса callbackList.get(0).callback()");
                callbackList.get(0).callback(); //обновляем интерфейс
            } else if (cmd == ListCommandByte.AUTH_ERR.getByteValue()) {
                GUIHelper.showError(new RuntimeException("Неверный логин или пароль"));
            }
        } else {
            GUIHelper.showError(new RuntimeException("Неверный контрольный байт"));
        }
    }

    public static void sendAuthInfo(Channel channel, String login, String password) {
        byte[] loginBytes = login.getBytes();
        byte[] passwordBytes = password.getBytes();
        int bufLength = 1 + 4 + loginBytes.length + 4 + passwordBytes.length;
        logger.debug("sendAuthInfo Высчитали размер буфера = " + bufLength+ "создаем буфер см.ниже");
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(bufLength);
        buf.writeByte(ListSignalByte.CMD_SIGNAL_BYTE);
        logger.debug("sendAuthInfo 1 writeByte SignalByte: " + ListSignalByte.CMD_SIGNAL_BYTE);
        buf.writeInt(login.length());
        logger.debug("sendAuthInfo 4 writeInt login.length: " + login.length());
        buf.writeBytes(loginBytes);
        logger.debug("sendAuthInfo writeBytes loginBytes: " + login);
        buf.writeInt(password.length());
        logger.debug("sendAuthInfo 4 writeInt password.length: " + password.length());
        buf.writeBytes(passwordBytes);
        logger.debug("sendAuthInfo writeBytes passwordBytes: " + password);
        channel.writeAndFlush(buf);
        logger.debug("sendAuthInfo channel.writeAndFlush(buf) отправили данные в канал");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        logger.error(cause);
        ctx.close();
    }
}
