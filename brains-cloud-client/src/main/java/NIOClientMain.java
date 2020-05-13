
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class NIOClientMain implements Runnable {
    private SocketChannel socketChannel;
    private ByteBuffer buf = ByteBuffer.allocate(256);
    private ByteBuffer welcomeBuf = ByteBuffer.wrap("сообщение!\n".getBytes());
    private Scanner in = new Scanner(System.in);
    private String text = new String();
    NIOClientMain() throws IOException {
        this.socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 8188));
        this.socketChannel.configureBlocking(false);
    }

    @Override
    public void run() {
        try (RandomAccessFile srcFile = new RandomAccessFile("channel_copy_ex_src.txt", "rw")) {
            FileChannel srcChannel = srcFile.getChannel();
            long position = 0;
            long size = srcChannel.size();
            srcChannel.transferTo(position, size, socketChannel);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            while (true){
                System.out.print("enter msg: ");
                text = in.nextLine();
                if(text.equals("exit")) break;
                welcomeBuf = ByteBuffer.wrap(text.getBytes());
                socketChannel.write(welcomeBuf);
                welcomeBuf.rewind();
            }
            socketChannel.finishConnect();

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            in.close();
        }
    }

    public static void main(String[] args) throws IOException {
        new Thread(new NIOClientMain()).start();
    }
}
