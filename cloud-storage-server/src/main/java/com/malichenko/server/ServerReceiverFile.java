package com.malichenko.server;

import com.malichenko.common.ListCommandByte;
import com.malichenko.common.ReceiverFile;
import com.malichenko.common.Sender;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.List;

public class ServerReceiverFile extends ReceiverFile {
    private Path filePath;
    private Path userPath;
    private Logger logger;

    public ServerReceiverFile(Path userPath) {
        super(userPath);
        this.userPath = userPath;
        this.logger = LogManager.getLogger();
    }

    @Override
    public void callback(ChannelHandlerContext ctx) {
        logger.debug("callback SERVER_FILES_LIST_INFO отправляем лист файлов клиенту userPath:" + userPath);
        Sender.sendFilesList(userPath, ctx.channel(), ListCommandByte.SERVER_FILES_LIST_INFO);
    }
}
