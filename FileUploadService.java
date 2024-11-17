import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class FileUploadService extends Microservice implements Runnable {
    private boolean isRunning;
    private ServerSocket serverSocket;

    public FileUploadService(String ipAddress, String port) {
        super(ipAddress, port);
    }

    @Override
    public int handleMessage() {
        return 0;
    }

    public String getServiceName() {
        return "file_upload";
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(Integer.parseInt(this.getPort()));
        isRunning = true;
        new Thread(this).start();
        System.out.println("FileUploadService started on port " + this.getPort());
    }

    public void stop() throws IOException {
        isRunning = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        System.out.println("FileUploadService stopped.");
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("FileUploadService accepted connection from " + clientSocket.getInetAddress());
                handleClientConnection(clientSocket);
            } catch (IOException e) {
                System.out.println("Error accepting connection: " + e.getMessage());
            }
        }
    }

    private void handleClientConnection(Socket clientSocket) {
        try (
                ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream())
        ) {
            String fileName = null;
            FileOutputStream fos = null;
            int totalPackages = 0;
            int receivedPackages = 0;
            String username=null;
            while (true) {
                Request request = (Request) objectInputStream.readObject();
                    if (fileName == null) {
                        fileName = request.getContent("filename").entryContent;
                        totalPackages = Integer.parseInt(request.getContent("total_packages").entryContent);
                        username = request.getContent("username").getEntryContent();
                        Path userDir = Paths.get("uploads", username);
                        if (Files.notExists(userDir)) {
                            try {
                                Files.createDirectories(userDir);
                                System.out.println("Created directory for user: " + username);
                            } catch (IOException e) {
                                System.out.println(e.getMessage());
                            }
                        }
                        fos = new FileOutputStream(userDir.resolve(fileName).toString());
                    }
                    String packageData = request.getContent("data").entryContent;
                    byte[] decodedData = Base64.getDecoder().decode(packageData);
                    fos.write(decodedData);
                    receivedPackages++;
                    System.out.println("Received package " + receivedPackages + " of " + totalPackages);
                    Request ack = new Request("ack", 1);
                    objectOutputStream.writeObject(ack);
                    objectOutputStream.flush();
                    if (receivedPackages == totalPackages) {
                        System.out.println("All packages received for file: " + fileName);
                        Request res = new Request("ack",1);
                        res.setMessageCode("200");
                        objectOutputStream.writeObject(res);
                        objectOutputStream.flush();
                        break;
                    }
                    else {
                        Request res = new Request("ack",1);
                        res.setMessageCode("100");
                        objectOutputStream.writeObject(res);
                        objectOutputStream.flush();
                    }

            }

            if (fos != null) {
                fos.close();
            }
        } catch (Exception e) {
            System.out.println("Error handling client connection: " + e.getMessage());
        }
    }



}
