import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Manager implements Runnable {
    private ServerSocket managerSocket;
    private boolean isRunning = false;
    private Map<String, AgentInfo> registeredAgents = new ConcurrentHashMap<>();
    private ExecutorService executorService = Executors.newCachedThreadPool();

    // Inner class to store agent information
    private class AgentInfo {
        Socket socket;
        String ipAddress;
        String port;
        long lastHeartbeat;
        List<String> availableServices;

        AgentInfo(Socket socket, String ipAddress, String port) {
            this.socket = socket;
            this.ipAddress = ipAddress;
            this.port = port;
            this.lastHeartbeat = System.currentTimeMillis();
            this.availableServices = new ArrayList<>();
        }
    }

    public Manager(int port) throws IOException {
        managerSocket = new ServerSocket(port);
        isRunning = true;
    }

    @Override
    public void run() {
        // Start heartbeat monitoring thread
        executorService.submit(this::monitorAgentHeartbeats);

        while (isRunning) {
            try {
                Socket clientSocket = managerSocket.accept();
                executorService.submit(() -> handleClientConnection(clientSocket));
            } catch (IOException e) {
                System.out.println("Error accepting client connection: " + e.getMessage());
            }
        }
    }

    private void handleClientConnection(Socket clientSocket) {
        try (ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream())) {

            Request request = (Request) inputStream.readObject();

            switch (request.getRequestType()) {
                case "agent_register":
                    registerAgent(request, clientSocket, outputStream);
                    break;
                case "service_request":
                    handleServiceRequest(request, outputStream);
                    break;
                case "heartbeat_report":
                    processHeartbeatReport(request, clientSocket);
                    break;
                default:
                    sendErrorResponse(outputStream, "Unknown request type");
            }
        } catch (Exception e) {
            System.out.println("Error handling client connection: " + e.getMessage());
        }
    }

    private void registerAgent(Request request, Socket socket, ObjectOutputStream outputStream) throws IOException {
        String ipAddress = request.getContent("ip_address").entryContent;
        String port = request.getContent("port").entryContent;
        List<String> services = parseServices(request.getContent("services").entryContent);

        AgentInfo agentInfo = new AgentInfo(socket, ipAddress, port);
        agentInfo.availableServices = services;
        registeredAgents.put(ipAddress + ":" + port, agentInfo);
        Request response = new Request("agent_registration_success", request.getRequestID());
        response.addEntry("message", "Agent registered successfully");
        outputStream.writeObject(response);
        System.out.println("Agent registered successfully");
        outputStream.flush();
    }

    private void handleServiceRequest(Request request, ObjectOutputStream outputStream) throws IOException {
        String requiredService = request.getContent("service_type").entryContent;
        AgentInfo selectedAgent = findAgentWithService(requiredService);

        if (selectedAgent != null) {
            Request serviceAllocation = new Request("service_allocation", request.getRequestID());
            serviceAllocation.addEntry("service_type", requiredService);
            serviceAllocation.addEntry("service_address", selectedAgent.ipAddress);
            serviceAllocation.addEntry("service_port", selectedAgent.port);
            outputStream.writeObject(serviceAllocation);
        } else {
            sendErrorResponse(outputStream, "No agent available for service: " + requiredService);
        }
        outputStream.flush();
    }

    private void processHeartbeatReport(Request request, Socket socket) {
        // Update last heartbeat timestamp for the agent
        String agentKey = socket.getInetAddress().toString() + ":" + socket.getPort();
        AgentInfo agentInfo = registeredAgents.get(agentKey);
        if (agentInfo != null) {
            agentInfo.lastHeartbeat = System.currentTimeMillis();
        }
    }

    private void monitorAgentHeartbeats() {
        while (isRunning) {
            long currentTime = System.currentTimeMillis();
            registeredAgents.entrySet().removeIf(entry ->
                    currentTime - entry.getValue().lastHeartbeat > 30000 // 30 seconds timeout
            );

            try {
                Thread.sleep(10000); // Check every 10 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private AgentInfo findAgentWithService(String serviceType) {
        return registeredAgents.values().stream()
                .filter(agent -> agent.availableServices.contains(serviceType))
                .findFirst()
                .orElse(null);
    }

    private List<String> parseServices(String servicesString) {
        return Arrays.asList(servicesString.split(","));
    }

    private void sendErrorResponse(ObjectOutputStream outputStream, String errorMessage) throws IOException {
        Request errorResponse = new Request("error", -1);
        errorResponse.addEntry("error_message", errorMessage);
        outputStream.writeObject(errorResponse);
        outputStream.flush();
    }

    public void stop() {
        isRunning = false;
        try {
            managerSocket.close();
        } catch (IOException e) {
            System.out.println("Error closing manager socket: " + e.getMessage());
        }
        executorService.shutdown();
    }

    public static void main(String[] args) {
        try {
            Manager manager = new Manager(8000); // Default port 8000
            new Thread(manager).start();
            System.out.println("Manager started on port 8000");
        } catch (IOException e) {
            System.out.println("Could not start manager: " + e.getMessage());
        }
    }
}