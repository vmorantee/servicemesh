import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Agent implements Runnable
{
    private String ipAddress;
    private String port;
    private boolean isRunning;
    private ServerSocket serverSocket;
    private Map<String, ServiceInfo> activeServices = new ConcurrentHashMap<>();

    public Agent(String ipAddress, String port)
    {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public void start() {
        isRunning = true;
        new Thread(this).start();
        System.out.println("Agent started on " + ipAddress + ":" + port);
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing server socket: " + e.getMessage());
        }
        System.out.println("Agent stopped.");
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(Integer.parseInt(port));
            System.out.println("Agent listening on port " + port);

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + clientSocket.getInetAddress());
                new Thread(() -> handleClientConnection(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("Error starting Agent: " + e.getMessage());
        }
    }

    private void handleClientConnection(Socket clientSocket) {
        try (
                ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream())
        ) {
            Request request = (Request) objectInputStream.readObject();
            System.out.println("Received request: " + request);
            Request response = new Request(request.getRequestType(), request.getRequestID());
            ///////////////////////
            
            ServiceInfo info = activeServices.get(request.getContent("serviceName").getEntryContent());

            if(info == null)
            {
                File mainFile = new File("services");
                File[] listOfFiles = mainFile.listFiles();
                mainFile = null;

                for(File file : listOfFiles)
                {
                    if(file.getName() == request.getContent("serviceName").getEntryContent())
                    {
                        mainFile = file;
                        break;
                    }
                }

                //Start service (java file)
                //if !service.isRunning() throw exception czy coś idk xd
            }

            response.addEntry("Adress", ipAddress);
            response.addEntry("port", port);
            response.setMessageCode("200");




            ///////////////////////
            objectOutputStream.writeObject(response);
            objectOutputStream.flush();
            System.out.println("Service found, response sent");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error handling client connection in Agent: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing client socket in Agent: " + e.getMessage());
            }
        }
    }
}