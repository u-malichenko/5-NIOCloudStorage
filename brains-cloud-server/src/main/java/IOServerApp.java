import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class IOServerApp {
    private static final int PORT = 8188;

    public IOServerApp() {
        System.out.println("Сервер запущен (Порт: " + PORT + ")");
        while (true) {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                Socket socket = serverSocket.accept();
                System.out.println("Клиент подключился");
                new IOServer(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new IOServerApp();
    }
}
