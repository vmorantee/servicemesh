import java.io.*;
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
    private ObjectOutputStream managerOutput;
    private  ObjectInputStream managerInput;

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
            managerOutput = new ObjectOutputStream(managerSocket.getOutputStream());
            managerInput = new ObjectInputStream(managerSocket.getInputStream());
            managerOutput.writeObject(registrationRequest);
            managerOutput.flush();
            managerOutput.reset();

            Request request = new Request("Huj",2626);
            request.addEntry("Huj","Dupa");
            managerOutput.writeObject(request);
            managerOutput.flush();
            try {
                Request response = (Request) managerInput.readObject();
                System.out.println(response);
            } catch (Exception e){
                System.out.println(e);
            }

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
                    ObjectOutputStream outputStream = managerOutput;
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
            System.out.println("Możliwe serwisy:");
            for(String serw : possibleServices)
                    System.out.println(serw);
            if(possibleServices.contains("ApiGateway"))
                forwardToManager(request, outputStream);
            else if (possibleServices.contains(request.getContent("service_type").getEntryContent())) {
                startServiceFromManager(request);
            } else if ("no_service_available".equals(request.getContent("service_type").getEntryContent())) {
                System.out.println("Sigma");
                forwardToManager(request, outputStream);
            } else if ("heartbeat".equals(request.getContent("service_type").getEntryContent())) {
                heartbeats.put(serviceSocket, request);
            }
            System.out.println("bruh moment");
        } catch (Exception e) {
            System.out.println("Error handling service message in handling: " + e.getMessage());
        }
    }

    private void forwardToManager(Request request, ObjectOutputStream outputStream) throws IOException {
        if (managerSocket != null) {


            managerOutput.writeObject(request);
            System.out.println("Test1");
            managerOutput.flush();
            managerOutput.reset();

            System.out.println("Test2");
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
        System.out.println("DLACZEGO NIE WCHODZI DO TRYA");
        try {
            System.out.println("Wystartuj serwis!!!");
            String serviceType = request.getContent("service_type").entryContent;
            String serviceAddress = request.getContent("service_address").entryContent;
            int servicePort = Integer.parseInt(request.getContent("service_port").entryContent);
            ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", serviceType + ".jar", serviceAddress, Integer.toString(servicePort), ipAddress, port); //TODO zamienić port na zmienną
            Process serviceProcess = processBuilder.start();
            System.out.println("Started service: " + serviceType+serviceAddress+servicePort);
            try{
                sleep(1000);
            } catch (Exception e){
                System.out.println("XD");
            }
            // Odczytanie standardowego wyjścia (stdout) procesu
            BufferedReader reader = new BufferedReader(new InputStreamReader(serviceProcess.getInputStream()));
            String line;
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
