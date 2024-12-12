import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ApiGateway implements Runnable {
    private String ipAddress;
    private String port;
    private boolean isRunning;
    private ServerSocket serverSocket;
    private Map<String, ServiceConnection> activeConnections = new ConcurrentHashMap<>();

    public ApiGateway(String ipAddress, String port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public void start() {
        isRunning = true;
        new Thread(this).start();
        System.out.println("API Gateway started on " + ipAddress + ":" + port);
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
        System.out.println("API Gateway stopped.");
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(Integer.parseInt(port));
            System.out.println("API Gateway listening on port " + port);

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + clientSocket.getInetAddress());
                new Thread(() -> handleClientConnection(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("Error starting API Gateway: " + e.getMessage());
        }
    }

    private void handleClientConnection(Socket clientSocket) {
        try (
                ObjectInputStream clientInputStream = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream clientOutputStream = new ObjectOutputStream(clientSocket.getOutputStream())
        ) {
            while (true) {
                try {
                    Request clientRequest = (Request) clientInputStream.readObject();
                    System.out.println("Received Request: " + clientRequest);
                    ServiceConnection serviceConnection = getConnection(clientRequest.getRequestType());
                    if (serviceConnection == null) {
                        serviceConnection = establishServiceConnection(clientRequest.getRequestType());
                        if (serviceConnection == null) {
                            System.out.println("Service not found for request type: " + clientRequest.getRequestType());
                            continue;
                        }
                    }

                    serviceConnection.getOutputStream().writeObject(clientRequest);
                    serviceConnection.getOutputStream().flush();
                    Request serviceResponse = (Request) serviceConnection.getInputStream().readObject();
                    clientOutputStream.writeObject(serviceResponse);
                    clientOutputStream.flush();
                } catch (EOFException e) {
                    System.out.println("Client connection closed.");
                    break;
                } catch (Exception e) {
                    System.out.println("Error forwarding request: " + e.getMessage());
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Error handling client connection: " + e.getMessage());
        }
    }

    private ServiceConnection establishServiceConnection(String serviceName) {
        for (Microservice service : Microservice.getInstances()) {
            if (service.getServiceName().equals(serviceName)) {
                try {
                    Socket serviceSocket = new Socket(service.getIpAddress(), Integer.parseInt(service.getPort()));
                    ObjectOutputStream serviceOut = new ObjectOutputStream(serviceSocket.getOutputStream());
                    ObjectInputStream serviceIn = new ObjectInputStream(serviceSocket.getInputStream());
                    ServiceConnection serviceConnection = new ServiceConnection(serviceSocket, serviceOut, serviceIn);
                    activeConnections.put(serviceName, serviceConnection);
                    return serviceConnection;
                } catch (IOException e) {
                    System.out.println("Failed to connect to service: " + e.getMessage());
                }
            }
        }
        return null;
    }

    private ServiceConnection getConnection(String serviceName) {
        ServiceConnection connection = activeConnections.get(serviceName);
        if (connection != null && connection.isValid()) {
            return connection;
        }
        activeConnections.remove(serviceName);
        return null;
    }
    private static class ServiceConnection {
        private Socket socket;
        private ObjectOutputStream outputStream;
        private ObjectInputStream inputStream;

        public ServiceConnection(Socket socket, ObjectOutputStream outputStream, ObjectInputStream inputStream) {
            this.socket = socket;
            this.outputStream = outputStream;
            this.inputStream = inputStream;
        }

        public ObjectOutputStream getOutputStream() {
            return outputStream;
        }

        public ObjectInputStream getInputStream() {
            return inputStream;
        }

        public boolean isValid() {
            return socket != null && !socket.isClosed();
        }
    }

    public static void main(String[] args) {
        ApiGateway api = new ApiGateway("127.0.0.1","8084");
        api.start();
        api.run();
    }
}
