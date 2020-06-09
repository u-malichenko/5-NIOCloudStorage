package com.malichenko.client;

import com.malichenko.common.ListCommandByte;
import com.malichenko.common.ReceiverCommand;
import com.malichenko.common.ReceiverFile;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.List;

public class ClientReceiverFile extends ReceiverFile {
    private Path filePath;
    private Path userPath;
    private List<Callback> callbackList;
    private Logger logger;

    public ClientReceiverFile(Path userPath, List<Callback> callbackList) {
        super(userPath);
        this.userPath = userPath;
        this.callbackList = callbackList;
        this.logger = LogManager.getLogger();
    }

    @Override
    public void callback(ChannelHandlerContext ctx) {
        logger.debug("callback callbackList.get(2).callback() LOCAL_FILES_LIST обновлдяем лист файлов");
        callbackList.get(2).callback();
    }
}
