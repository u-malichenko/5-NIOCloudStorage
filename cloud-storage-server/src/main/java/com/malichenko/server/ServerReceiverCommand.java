package com.malichenko.server;

import com.malichenko.common.ListCommandByte;
import com.malichenko.common.ReceiverCommand;
import com.malichenko.common.Sender;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.nio.file.Path;

public class ServerReceiverCommand extends ReceiverCommand {
    private Path filePath;
    private Path userPath;
    private Logger logger;

    public ServerReceiverCommand(Path userPath) {
        this.userPath = userPath;
        this.logger = LogManager.getLogger();
    }

    @Override
    public void parseCommand(ChannelHandlerContext ctx, byte command, byte[] cmd) throws Exception {
        getFilePath(userPath, cmd);
        if (command == ListCommandByte.SERVER_FILES_LIST_INFO.getByteValue()) {
            logger.debug("parseCommand SERVER_FILES_LIST_INFO отправляем лист файлов клиенту userPath:" + userPath);
            Sender.sendFilesList(userPath, ctx.channel(), ListCommandByte.SERVER_FILES_LIST_INFO);
        }
        if (command == ListCommandByte.DELETE_FILE.getByteValue()) {
            if (filePath.toFile().delete()) {
                logger.debug("parseCommand DELETE_FILE пришла команда удалить файл (запускаем обновление листа файлов) filePath:" + filePath);
                Sender.sendFilesList(userPath, ctx.channel(), ListCommandByte.SERVER_FILES_LIST_INFO);
            } else {
                logger.error("parseCommand DELETE_FILE_ERR отправить команду -ошибка удаления файла filePath:" + filePath);
                Sender.sendCommand(ctx.channel(), ListCommandByte.DELETE_FILE_ERR);
            }
        }
        if (command == ListCommandByte.REQUEST_FILE.getByteValue()) {
            logger.debug("parseCommand REQUEST_FILE пришла команда -запрос файла на скачивание filePath:" + filePath);
            Sender.sendFile(filePath, ctx.channel(), future -> {
                if (!future.isSuccess()) {
                    future.cause().printStackTrace();
                    logger.debug("parseCommand REQUEST_FILE_ERR отправить команду об ошибке скачивания filePath:" + filePath);
                    Sender.sendCommand(ctx.channel(), ListCommandByte.REQUEST_FILE_ERR);
                }
                //todo возможно тут отправлять ответ на обновление списка файлов
            });
        }
    }

    /**
     * метод получения полного пути(с именем файла) папка пользователя + имя файла
     */
    private void getFilePath(Path userPath, byte[] fileName) {

        if(fileName == null) {
            filePath = userPath;
            return;
        }
        try {
                filePath = userPath.resolve(new String(fileName, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                logger.error(e);
            }
    }
}
