import java.io.File;
import java.io.IOException;

public class IOClientApp {
    public static void main(String[] args) throws IOException {
        IOClient IOClient = new IOClient();
        File file = new File("brains-cloud-client\\IO_test.txt");
        IOClient.sendFile(file);

    }

}
