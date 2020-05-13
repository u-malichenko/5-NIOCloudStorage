
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.Iterator;

public class NIOServerMain implements Runnable {
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private ByteBuffer buf = ByteBuffer.allocate(256);
    private int acceptedClientIndex = 1;
    private final ByteBuffer welcomeBuf = ByteBuffer.wrap("Добро пожаловать в файлообменник!\n".getBytes());
    private final ByteBuffer transferBuf = ByteBuffer.wrap("Файл принят \n".getBytes());

    NIOServerMain() throws IOException {
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.socket().bind(new InetSocketAddress(8189));
        this.serverSocketChannel.configureBlocking(false);
        this.selector = Selector.open();
        this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void run() {
        try {
            System.out.println("Сервер запущен (Порт: 8189)");
            Iterator<SelectionKey> iter;
            SelectionKey key;
            while (this.serverSocketChannel.isOpen()) {
                selector.select();
                iter = this.selector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    key = iter.next();
                    iter.remove();
                    if (key.isAcceptable()) this.handleAccept(key);
                    if (key.isReadable()) this.handleRead(key);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel sc = ((ServerSocketChannel) key.channel()).accept();
        String clientName = "Клиент #" + acceptedClientIndex;
        acceptedClientIndex++;
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ, clientName); //регистрация на селекторе
        sc.write(welcomeBuf); //посылаем клиенту в канал сообщение
        welcomeBuf.rewind();
        System.out.println("Подключился новый клиент " + clientName);
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        buf.clear();
        int read = 0;
        while ((read = clientChannel.read(buf)) > 0) {
            buf.flip();
            byte[] bytes = new byte[buf.limit()];
            buf.get(bytes);
            try {
                Files.write(Paths.get("temp.jpg"),bytes, StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
            buf.clear();
        }
        if (read < 0) {
            System.out.println(key.attachment() + " отключился\n");
            clientChannel.close();
        } else {
            Files.move(Paths.get("temp.jpg"), Paths.get("new.jpg"), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("получен файл от " + key.attachment());
            clientChannel.write(transferBuf);
            transferBuf.rewind();
        }
    }

    public static void main(String[] args) throws IOException {
        new Thread(new NIOServerMain()).start();
    }
}
