import java.net.*;

public class Client extends NetworkConnection {
    private String ip;

    public Client(String ip, java.util.function.Consumer<String> callback) {
        super(callback);
        this.ip = ip;
    }

    @Override
    protected Socket createSocket() throws Exception {
        return new Socket(ip, 55555);
    }
}