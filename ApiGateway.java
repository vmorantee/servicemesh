import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ApiGateway implements Runnable {
    private String ipAddress, agentIpAddress;
    private String port, agentPort;
    private boolean isRunning;
    private Socket agentSocket;
    private ServerSocket apiSocket;
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
            if (agentSocket != null && !agentSocket.isClosed()) {
                agentSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing server socket: " + e.getMessage());
        }
        System.out.println("API Gateway stopped.");
    }

    public void setAgentConnectionInfo(String ip, String port)
    {
        agentIpAddress = ip;
        agentPort = port;
    }

    @Override
    public void run() {
        try {
            agentSocket = new Socket(agentIpAddress, Integer.parseInt(agentPort));
            System.out.println("Connected to Service Manager at " + agentIpAddress + ":" + agentPort);
            System.out.println("Api gateway started (run())");

            try {
                apiSocket = new ServerSocket(Integer.parseInt(port));
                System.out.println("API Gateway listening on port " + port);

                while (isRunning) {
                    Socket clientSocket = apiSocket.accept();
                    System.out.println("Accepted connection from " + clientSocket.getInetAddress());
                    new Thread(() -> handleClientConnection(clientSocket)).start();
                }
            } catch (IOException e) {
                System.out.println("Error starting API Gateway: " + e.getMessage());
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
                    ServiceConnection service = activeConnections.get(clientRequest.getContent("service_type").getEntryContent());
                    if(service != null)
                        ServeTheClient(clientRequest, service.getSocket(), clientInputStream, clientOutputStream);
                    
                    else if (agentSocket != null) {
                        ObjectOutputStream agentOutput = new ObjectOutputStream(agentSocket.getOutputStream());
                        agentOutput.writeObject(clientRequest);
                        agentOutput.flush();
                        ObjectInputStream agentInput = new ObjectInputStream(agentSocket.getInputStream());
                        Request agentResponse = null;
                        try {
                            agentResponse = (Request) agentInput.readObject();
                            
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }

//                    ServiceConnection serviceConnection = getConnection(clientRequest.getRequestType());
//                    if (serviceConnection == null) {
//                        serviceConnection = establishServiceConnection(clientRequest.getRequestType());
//                        if (serviceConnection == null) {
//                            System.out.println("Service not found for request type: " + clientRequest.getRequestType());
//                            continue;
//                        }
//                    }
//
//                    serviceConnection.getOutputStream().writeObject(clientRequest);
//                    serviceConnection.getOutputStream().flush();
//                    Request serviceResponse = (Request) serviceConnection.getInputStream().readObject();
//                    clientOutputStream.writeObject(serviceResponse);
//                    clientOutputStream.flush();
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

    private void ServeTheClient(Request request, Socket serviceSocket, ObjectInputStream clientObjectInputStream, ObjectOutputStream clientObjectOutputStream) throws Exception
    {
        ObjectInputStream serviceInput = new ObjectInputStream(serviceSocket.getInputStream());
        ObjectOutputStream serviceOutput = new ObjectOutputStream(serviceSocket.getOutputStream());

        serviceOutput.writeObject(request);
        serviceOutput.flush();

        Request response = (Request) serviceInput.readObject();

        clientObjectOutputStream.writeObject(response);
        clientObjectOutputStream.flush();
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

    private void forwardToAgent(Request request, ObjectOutputStream outputStream) throws IOException {

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

        public Socket getSocket()
        {
            return this.socket;
        }

        public boolean isValid() {
            return socket != null && !socket.isClosed();
        }
    }

    public static void main(String[] args) {
        ApiGateway api;

        if(args.length == 4)
        {
            api = new ApiGateway(args[0],args[1]);
            api.setAgentConnectionInfo(args[2], args[3]);
        }
        else
        {
            api = new ApiGateway("127.0.0.1", "8090");
            api.setAgentConnectionInfo("127.0.0.1", "8086");
        }
        api.start();

        //api.run();
    }
}
