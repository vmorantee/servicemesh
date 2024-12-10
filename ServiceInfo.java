import java.net.Socket;

public class ServiceInfo {
    private final String type;
    private final String uid;
    private final Socket socket;

    public ServiceInfo(String type, String uid, Socket socket) {
        this.type = type;
        this.uid = uid;
        this.socket = socket;
    }

    public String getType() {
        return type;
    }

    public String getUid() {
        return uid;
    }

    public Socket getSocket() {
        return socket;
    }
}