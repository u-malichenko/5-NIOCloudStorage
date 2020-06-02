package com.malichenko.yury.netty.server;

import com.malichenko.yury.netty.common.*;
import io.netty.channel.ChannelHandlerContext;

import java.io.UnsupportedEncodingException;
import java.nio.file.Path;

public class ServerReceiverCommand extends ReceiverCommand {
    private Path filePath;
    private Path userPath;

    public ServerReceiverCommand(Path userPath) {
            this.userPath = userPath;
        }

    @Override
    public void parseCommand(ChannelHandlerContext ctx, byte command, byte[] cmd) throws Exception {
        getFilePath(userPath, cmd);
        if (command == ListCommandByte.SERVER_FILES_LIST_INFO.getByteValue()) {
            Sender.sendFilesList(userPath, ctx.channel(), ListCommandByte.SERVER_FILES_LIST_INFO);
        }
        if (command == ListCommandByte.DELETE_FILE.getByteValue()) {
            if (filePath.toFile().delete()) {
                Sender.sendFilesList(userPath, ctx.channel(), ListCommandByte.SERVER_FILES_LIST_INFO);
            } else {
                Sender.sendCommand(ctx.channel(), ListCommandByte.DELETE_FILE_ERR);
            }
        }
        if (command == ListCommandByte.REQUEST_FILE.getByteValue()) {
            Sender.sendFile(filePath, ctx.channel(), future -> {
                if (!future.isSuccess()) {
                    future.cause().printStackTrace();
                    Sender.sendCommand(ctx.channel(), ListCommandByte.REQUEST_FILE_ERR);
                }
            });
        }else {
            System.out.println("ERROR: Invalid first byte - " + command);
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
