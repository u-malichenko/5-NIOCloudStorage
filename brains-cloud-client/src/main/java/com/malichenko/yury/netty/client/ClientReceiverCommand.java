package com.malichenko.yury.netty.client;

import com.malichenko.yury.netty.common.ListCommandByte;
import com.malichenko.yury.netty.common.ReceiverCommand;
import io.netty.channel.ChannelHandlerContext;


import java.io.UnsupportedEncodingException;
import java.nio.file.Path;

import java.util.List;

public class ClientReceiverCommand extends ReceiverCommand {
    private Path filePath;
    private Path userPath;
    private List<Callback> callbackList;

    public ClientReceiverCommand(Path userPath, List<Callback> callbackList) {
        this.userPath = userPath;
        this.callbackList = callbackList;
    }

    @Override
    public void parseCommand(ChannelHandlerContext ctx,  byte command, byte[] cmd) {
        getFilePath(userPath, cmd);
        if (command == ListCommandByte.DELETE_FILE_ERR.getByteValue()) {
            GUIHelper.showError(new RuntimeException("Не удалось удалить файл"));
        }
        else if (command == ListCommandByte.REQUEST_FILE_ERR.getByteValue()) {
            GUIHelper.showError(new RuntimeException("Не удалось скачать файл"));
        } else {
            System.out.println("ERROR: Invalid second command byte - " + command);
        }
    }

    /**
     * метод получения полного пути(с именем файла) папка пользователя + имя файла
     */
    private void getFilePath(Path userPath, byte[] fileName) {
        try {
            filePath = userPath.resolve(new String(fileName, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

}
