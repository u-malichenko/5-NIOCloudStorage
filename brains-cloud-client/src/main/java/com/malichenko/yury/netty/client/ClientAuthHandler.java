package com.malichenko.yury.netty.client;

import com.malichenko.yury.netty.common.ListCommandByte;
import com.malichenko.yury.netty.common.ListSignalByte;
import com.malichenko.yury.netty.common.Handler;
import io.netty.buffer.ByteBuf;
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
    private Logger logger;
    private Path userPath;
    private String userDirectory;
    private List<Callback> callbackList;

    public ClientAuthHandler(List<Callback> callbackList) {
        logger = LogManager.getLogger();
        this.callbackList = callbackList;
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = ((ByteBuf) msg);
        byte controlByte = buf.readByte();
        if (controlByte == ListSignalByte.CMD_SIGNAL_BYTE) {
            byte command = buf.readByte();
            if (command == ListCommandByte.AUTH_OK.getByteValue()) {
                System.out.println("Добро пожаловать!");
                userDirectory = GUIHelper.userDirectory; //вытаскиваем из временного хранилища переменную папки пользователя
                userPath = Paths.get("client_storage", userDirectory);
                ctx.pipeline().addLast(new Handler(userPath, new ClientReceiverCommand(userPath, callbackList), new ClientReceiverListFile(userPath, callbackList)));
                ctx.pipeline().remove(this);
                logger.debug("Пользователь : " + userDirectory + " успешно прошел авторизацию.");
                callbackList.get(0).callback(); //обновляем интерфейс
            } else if (command == ListCommandByte.AUTH_ERR.getByteValue()) {
                GUIHelper.showError(new RuntimeException("Неверный логин или пароль"));
            }
        }else{
            GUIHelper.showError(new RuntimeException("Неверный контрольный байт"));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        logger.error(cause);
        ctx.close();
    }
}
