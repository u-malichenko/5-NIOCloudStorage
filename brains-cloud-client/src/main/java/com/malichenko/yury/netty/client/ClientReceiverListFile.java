package com.malichenko.yury.netty.client;

import com.malichenko.yury.netty.common.ReceiverListFile;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ClientReceiverListFile extends ReceiverListFile {
    public enum State {
        IDLE, LIST_SIZE, NAME_LENGTH_LIST, NAME_LIST, FILE_LENGTH_LIST
    }

    private int listSize;
    private int nameSize;
    private String fileName;
    private String userDirectory;
    private Path userPath;
    private Path filePath;
    private State currentState;
    List<Callback> callbackList;


    public ClientReceiverListFile(Path userPath, List<Callback> callbackList) {
        this.currentState = State.IDLE;
        this.userPath = userPath;
        this.callbackList = callbackList;
        this.userDirectory = GUIHelper.userDirectory;
    }

    public void startReceive() {
        if (currentState == State.IDLE) {
            GUIHelper.serverFilesList.clear();
            currentState = State.LIST_SIZE;
            System.out.println("STATE: Start list file receiving");
        }
    }

    public void receive(ChannelHandlerContext ctx, ByteBuf buf, Runnable finishOperation) throws Exception {

        if (currentState == State.LIST_SIZE) {
            if (buf.readableBytes() >= 4) {
                listSize = buf.readInt();
                System.out.println("STATE: LIST_SIZE listSize" + listSize);
                currentState = State.NAME_LENGTH_LIST;
            }
        }
        if (currentState == State.NAME_LENGTH_LIST) {
            if (buf.readableBytes() >= 4) {
                nameSize = buf.readInt();
                System.out.println("STATE: NAME_LENGTH_LIST nameSize " + nameSize);
                currentState = State.NAME_LIST;
            }
        }
        if (currentState == State.NAME_LIST) {
            if (buf.readableBytes() >= nameSize) {
                byte[] nextFileNameBytes = new byte[nameSize];
                buf.readBytes(nextFileNameBytes);
                getFileName(nextFileNameBytes);
                System.out.println("STATE: NAME_LIST fileName " + fileName);
                currentState = State.FILE_LENGTH_LIST;
            }
        }
        if (currentState == State.FILE_LENGTH_LIST) {
            if (buf.readableBytes() >= 8) {
                long fileSize = buf.readLong();
                System.out.println("STATE: FILE_LENGTH_LIST fileSize " + fileSize);
                filePath = Paths.get("server_storage", userDirectory, fileName);
                FileInfo fileInfo = new FileInfo(filePath.toFile(), fileName, fileSize);
                GUIHelper.serverFilesList.add(fileInfo);
                System.out.println("STATE: GUIHelper.serverFilesList.add filePath " + filePath);
                listSize--; //уменьшаем счетчик файлов(строк) в списке файлов
                currentState = State.NAME_LENGTH_LIST; //сбрасываем состояние NAME_LENGTH_LIST для следующей итерации получения файла
            }
        }
        if (listSize == 0) {
            callbackList.get(1).callback();
            System.out.println("List file received");
            currentState = State.IDLE;
            finishOperation.run();
        }
    }

    /**
     * метод получения имени файла из байт
     */
    private void getFileName(byte[] nextFileNameBytes) {
        try {
            fileName = new String(nextFileNameBytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
