import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public abstract class NetworkConnection {
    private ConnectionThread connThread = new ConnectionThread();
    private Consumer<String> onReceiveCallback;

    public NetworkConnection(Consumer<String> onReceiveCallback) {
        this.onReceiveCallback = onReceiveCallback;
        connThread.setDaemon(true);
    }

    public void start() throws Exception {
        connThread.start();
    }

    public void send(String data) throws Exception {
        connThread.out.writeUTF(data);
        connThread.out.flush();
    }
    private volatile boolean isConnected = false;

    public boolean isReady() {
        return isConnected;
    }


    protected abstract Socket createSocket() throws Exception;

    private class ConnectionThread extends Thread {
        private DataInputStream in;
        private DataOutputStream out;

        public void run() {
            try (Socket socket = createSocket()) {
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
                isConnected = true;

                while (true) {
                    String message = in.readUTF();
                    onReceiveCallback.accept(message);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}