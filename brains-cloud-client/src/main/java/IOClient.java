import java.io.*;
import java.net.Socket;

public class IOClient implements Closeable {
    private final String serverAddress;
    private final int port;

    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    public IOClient() {
        this.serverAddress = "localhost";
        this.port = 8188;
    }

    private void initConnect(String serverAddress, int port) throws IOException {
        this.socket = new Socket(serverAddress, port);
        this.inputStream = new DataInputStream(socket.getInputStream());
        this.outputStream = new DataOutputStream(socket.getOutputStream());
        Thread connectServerThread = new Thread(() -> {
            while (true) {
                try {
                    String message = inputStream.readUTF();
                    System.out.println(message);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });
        connectServerThread.setDaemon(true);
        connectServerThread.start();
        System.out.println("Клиент подключился к серверу: "+serverAddress+" на порт: "+port);
    }


    public void sendFile(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            if (outputStream == null) initConnect(serverAddress, port);

            int nameSize = file.getName().getBytes().length;
            outputStream.writeInt(nameSize);
            outputStream.writeUTF(file.getName());
            long fileSize = file.length();
            outputStream.writeLong(fileSize);
            int memSize = 1024;
            byte[] mem = new byte[memSize];
            while (fileSize / memSize >= 1) {
                fis.read(mem);
                outputStream.write(mem);
                fileSize -= memSize;
            }
            byte[] endMem = new byte[(int) fileSize % memSize];
            fis.read(endMem);
            outputStream.write(endMem);
            System.out.println("Клиент отправил на сервер файл: "+file.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
