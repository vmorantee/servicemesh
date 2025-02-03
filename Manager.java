import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Manager implements Runnable {
    private ServerSocket managerSocket;
    private boolean isRunning = false;
    private Map<String, AgentInfo> registeredAgents = new ConcurrentHashMap<>();
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private int newServicePort = 8101;

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

        @Override
        public String toString() {
            return "AgentInfo{" +
                    ", ipAddress='" + ipAddress + '\'' +
                    ", port='" + port + '\'' +
                    ", availableServices=" + availableServices +
                    '}';
        }

        public List<String> getAvailableServices() {
            return availableServices;
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

    private int findFreePort(int startPort) {
        int port = startPort;
        while (port <= 65535) { // Maksymalny zakres portów TCP
            try (ServerSocket socket = new ServerSocket(port)) {
                return port; // Jeśli udało się otworzyć, port jest wolny
            } catch (IOException ignored) {
                // Port jest zajęty, sprawdzamy następny
                port++;
            }
        }
        throw new RuntimeException("Nie znaleziono wolnego portu w zakresie od " + startPort + " do 65535");
    }

    private void handleClientConnection(Socket clientSocket) {
        try (ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream())) {
                while(true){
            Request request = (Request) inputStream.readObject();
            System.out.println(request.getRequestType());
            System.out.println(request);

            switch (request.getRequestType()) {
                case "agent_register":
                    registerAgent(request, clientSocket, outputStream);
                    break;
                case "heartbeat_report":
                    processHeartbeatReport(request, clientSocket);
                    break;
                default:
                    handleServiceRequest(request, outputStream);
            }
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
        System.out.println(ipAddress + ":" + port);
        System.out.println(agentInfo);
    }

    private void handleServiceRequest(Request request, ObjectOutputStream outputStream) throws IOException {
        String requiredService = request.getContent("service_type").entryContent;
        System.out.println("Looking for service with: "+ requiredService);
        AgentInfo selectedAgent = findAgentWithService(requiredService);
        System.out.println("Found: "+selectedAgent);

        if (selectedAgent != null) {
            Request serviceAllocation = new Request("service_allocation", request.getRequestID());
            serviceAllocation.addEntry("service_type", requiredService);
            serviceAllocation.addEntry("service_address", selectedAgent.ipAddress);
            serviceAllocation.addEntry("service_port", Integer.toString(findFreePort(8100)));
            Socket agentSocket = new Socket(selectedAgent.ipAddress, Integer.parseInt(selectedAgent.port));
            ObjectOutputStream agentOutput = new ObjectOutputStream(agentSocket.getOutputStream());
            ObjectInputStream agentInput = new ObjectInputStream(agentSocket.getInputStream());
            agentOutput.writeObject(serviceAllocation);
            agentOutput.flush();
            try {
                Request response = (Request) agentInput.readObject();
                outputStream.writeObject(response);
                outputStream.flush();
            } catch (Exception e) {
                System.out.println(e);
            }
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
                .filter(agent -> agent.getAvailableServices().contains(serviceType))
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