import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("127.0.0.1", 8103);
            Request r = new Request("siema", 120);
            r.addEntry("test", "test");
            ObjectOutputStream o = new ObjectOutputStream(socket.getOutputStream());
            o.writeObject(r);
            o.flush();
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}