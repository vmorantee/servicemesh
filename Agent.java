import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Vector;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

public abstract class Agent implements Runnable {
    private boolean isRunning = false;
    private String ipAddress, port;
    private HashMap<String, Socket> activeServices = new HashMap<>();
    private Vector<String> possibleServices = new Vector<>();
    private HashMap<Socket, Request> heartbeats = new HashMap<>();
    private Socket managerSocket;
    private ServerSocket agentSocket;
    private ServerSocket newServiceSocket;
    private String managerIpAddress;
    private int managerPort;
    public abstract String getAgentName();

    public Agent(String ipAddress, String port, String managerIpAddress, int managerPort) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.managerIpAddress = managerIpAddress;
        this.managerPort = managerPort;
    }
    public String getPort() {
        return port;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void start() throws IOException {
        // Existing start method code
        agentSocket = new ServerSocket(Integer.parseInt(this.getPort()));
        newServiceSocket = new ServerSocket(Integer.parseInt(this.getPort())+1);

        // Connect to manager immediately after starting
        connectToManager();

        isRunning = true;
        possibleServices = fillPossibleServices();
        new Thread(this).start();
        System.out.println("Agent started on port: " + this.getPort());
    }
    private void connectToManager() {
        try {
            // Establish connection to manager
            managerSocket = new Socket(managerIpAddress, managerPort);

            // Prepare registration request
            Request registrationRequest = new Request("agent_register", 1);
            registrationRequest.addEntry("ip_address", ipAddress);
            registrationRequest.addEntry("port", port);

            // Add available services to registration
            String servicesString = possibleServices.stream()
                    .collect(Collectors.joining(","));
            registrationRequest.addEntry("services", servicesString);

            // Send registration request
            ObjectOutputStream outputStream = new ObjectOutputStream(managerSocket.getOutputStream());
            outputStream.writeObject(registrationRequest);
            outputStream.flush();

            System.out.println("Connected to manager at " + managerIpAddress + ":" + managerPort);
        } catch (IOException e) {
            System.out.println("Error connecting to manager: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        new Thread(this::sendHeartbeatsToManager).start();
        while (isRunning) {
            try {
                Socket serviceSocket = agentSocket.accept();
                handleServiceMessage(serviceSocket);
            } catch (IOException e) {
                System.out.println("Error handling service connection: " + e.getMessage());
            }
        }
    }

    public abstract Vector<String> fillPossibleServices();

    private void sendHeartbeatsToManager() {
        while (isRunning) {
            try {
                sleep(10000);
                if (managerSocket != null && !heartbeats.isEmpty()) {
                    ObjectOutputStream outputStream = new ObjectOutputStream(managerSocket.getOutputStream());
                    Request heartbeatReport = new Request("heartbeat_report", 1);
                    for (Request heartbeat : heartbeats.values()) {
                        heartbeatReport.addEntry("heartbeat", heartbeat.toString());
                    }
                    outputStream.writeObject(heartbeatReport);
                    outputStream.flush();
                }
            } catch (Exception e) {
                System.out.println("Error sending heartbeats to manager: " + e.getMessage());
            }
        }
    }

    private void handleServiceMessage(Socket serviceSocket) {
        try (ObjectInputStream inputStream = new ObjectInputStream(serviceSocket.getInputStream());
             ObjectOutputStream outputStream = new ObjectOutputStream(serviceSocket.getOutputStream())) {
            Request request = (Request) inputStream.readObject();
            System.out.println(request);
            if (possibleServices.contains(request.getRequestType())) {
                startServiceFromManager(request);
            } else if ("no_service_available".equals(request.getRequestType())) {
                System.out.println("Sigma");
                forwardToManager(request, outputStream);
            } else if ("heartbeat".equals(request.getRequestType())) {
                heartbeats.put(serviceSocket, request);
            }
        } catch (Exception e) {
            System.out.println("Error handling service message: " + e.getMessage());
        }
    }

    private void forwardToManager(Request request, ObjectOutputStream outputStream) throws IOException {
        if (managerSocket != null) {
            ObjectOutputStream managerOutput = new ObjectOutputStream(managerSocket.getOutputStream());
            managerOutput.writeObject(request);
            managerOutput.flush();
            ObjectInputStream managerInput = new ObjectInputStream(managerSocket.getInputStream());
            Request managerResponse = null;
            try {
                managerResponse = (Request) managerInput.readObject();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            outputStream.writeObject(managerResponse);
            outputStream.flush();
        }
    }

    public void startServiceFromManager(Request request) {
        try {
            String serviceType = request.getContent("service_type").entryContent;
            String serviceAddress = request.getContent("service_address").entryContent;
            int servicePort = Integer.parseInt(request.getContent("service_port").entryContent);
            ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", serviceType + ".jar", serviceAddress, Integer.toString(servicePort), ipAddress, port); //TODO zamienić port na zmienną
            Process serviceProcess = processBuilder.start();
            System.out.println("Started service: " + serviceType+serviceAddress+servicePort);
            try{
                sleep(5000);
            } catch (Exception e){
                System.out.println("XD");
            }
            System.out.println("test");
            // Odczytanie standardowego wyjścia (stdout) procesu
            BufferedReader reader = new BufferedReader(new InputStreamReader(serviceProcess.getInputStream()));
            String line;
            System.out.println("test");
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            Socket serviceSocket = newServiceSocket.accept(); // PROBLEM HERE !!!!!!!!!
            System.out.println("Service succesfully connected to the agent");
            Request serviceDetails = new Request("service_started", 1);
            serviceDetails.addEntry("service_type", serviceType);
            serviceDetails.addEntry("service_socket", serviceSocket.getInetAddress().toString() + ":" + serviceSocket.getPort());
//            ObjectOutputStream managerOutput = new ObjectOutputStream(managerSocket.getOutputStream());
//            managerOutput.writeObject(serviceDetails);
//            managerOutput.flush();

        } catch (IOException e) {
            System.out.println("Error starting service: " + e.getMessage());
        }
    }



}
