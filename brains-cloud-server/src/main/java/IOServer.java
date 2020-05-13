import java.io.*;
import java.net.Socket;

public class IOServer {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public IOServer(Socket socket) {
        try {
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    readFile();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readFile() throws IOException {
        int nameSize = in.readInt();
        String fileName = in.readUTF();
        long fileSize = in.readLong();
        File file = new File("brains-cloud-server\\" + fileName);

        try(FileOutputStream fos = new FileOutputStream(file)){
            int memSize = 1024;
            byte[] mem = new byte[memSize];
            while (fileSize / memSize >= 1) {
                in.read(mem);
                fos.write(mem);

                fileSize -= memSize;
            }
            byte[] endMem = new byte[(int) fileSize % memSize];
            in.read(endMem);
            fos.write(endMem);
            System.out.println("Сервер принял файл: "+fileName);
        }

    }

    private void closeConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
