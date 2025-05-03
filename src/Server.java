import java.net.*;

public class Server extends NetworkConnection {
    public Server(java.util.function.Consumer<String> callback) {
        super(callback);
    }

    @Override
    protected Socket createSocket() throws Exception {
        ServerSocket serverSocket = new ServerSocket(55555);
        System.out.println("Waiting for client...");
        return serverSocket.accept();
    }
}