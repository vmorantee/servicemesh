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
        possibleServices = fillPossibleServices();
<<<<<<< HEAD
        System.out.println("Available services:");
        for(String serw : possibleServices)
            System.out.println(serw);
=======
>>>>>>> refs/remotes/origin/yetAnotherBranch


        // Connect to manager immediately after starting
        connectToManager();


        isRunning = true;
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
            if(possibleServices.contains("ApiGateway"))
                forwardToManager(request, outputStream);
            else if (possibleServices.contains(request.getContent("service_type").getEntryContent())) {
                startServiceFromManager(request, outputStream, inputStream);
            } else if ("no_service_available".equals(request.getContent("message").getEntryContent())) {
                System.out.println("Sigma");
                forwardToManager(request, outputStream);
            } else if ("heartbeat".equals(request.getContent("service_type").getEntryContent())) {
                heartbeats.put(serviceSocket, request);
            }
        } catch (Exception e) {
            System.out.println("Error handling service message in handling: " + e.getMessage());
        }
    }

    private void forwardToManager(Request request, ObjectOutputStream outputStream) throws IOException {
        if (managerSocket != null) {
            managerOutput.writeObject(request);
            managerOutput.flush();
            managerOutput.reset();

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

    public void startServiceFromManager(Request request, ObjectOutputStream outputStream, ObjectInputStream inputStream) {
        try {
            System.out.println(request);
            String serviceType = request.getContent("service_type").entryContent;
            String serviceAddress = request.getContent("service_address").entryContent;
            int servicePort = Integer.parseInt(request.getContent("service_port").entryContent);

            // Ścieżka do pliku .java
            String javaFilePath = serviceType + ".java"; // np. "ApiGateway.java"

            // 1. Skompiluj plik .java
            ProcessBuilder compileProcessBuilder = new ProcessBuilder("javac", javaFilePath);
            compileProcessBuilder.redirectErrorStream(true); // Przekieruj błędy do standardowego wyjścia

            Process compileProcess = compileProcessBuilder.start();
            int compileExitCode = compileProcess.waitFor(); // Poczekaj na zakończenie kompilacji

            if (compileExitCode != 0) {
                System.out.println("Compilation failed with exit code: " + compileExitCode);
                // Odczytaj błędy kompilacji
                BufferedReader compileErrorReader = new BufferedReader(new InputStreamReader(compileProcess.getErrorStream()));
                String line;
                while ((line = compileErrorReader.readLine()) != null) {
                    System.out.println(line);
                }
                return; // Zakończ metodę, jeśli kompilacja się nie powiodła
            }

            System.out.println("Compilation successful!");
            compileProcess.destroy();
            compileProcess.destroyForcibly();


            String className = serviceType; // Nazwa klasy (bez .java)
            ProcessBuilder runProcessBuilder = new ProcessBuilder("java", className, serviceAddress, Integer.toString(servicePort), ipAddress);
            runProcessBuilder.redirectErrorStream(true); // Przekieruj błędy do standardowego wyjścia

            Process runProcess = runProcessBuilder.start();
            BufferedReader runOutputReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
            new Thread(() -> {
                String line;
                while (true) {
                    try {
                        if (!((line = runOutputReader.readLine()) != null)) break;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println(line);
                }
            }).start();

            System.out.println("Started service: " + serviceType+serviceAddress+servicePort);
            //runProcess.destroy();
            //runProcess.destroyForcibly();
        
            Request responseToManager = new Request(request.getRequestType(), request.getRequestID());
            responseToManager.addEntry("service_type", request.getContent("service_type").entryContent);
            responseToManager.addEntry("service_address", request.getContent("service_address").entryContent);
            responseToManager.addEntry("service_port", request.getContent("service_port").entryContent);

            outputStream.writeObject(responseToManager);
            outputStream.flush();

            // Odczytanie standardowego wyjścia (stdout) procesu
            //BufferedReader reader = new BufferedReader(new InputStreamReader(serviceProcess.getInputStream()));
            //String line;
            //while ((line = reader.readLine()) != null) {
            //    System.out.println(line);
            //}
            //System.out.println("XD");
            //Socket serviceSocket = newServiceSocket.accept(); // PROBLEM HERE !!!!!!!!!
            //System.out.println("XD");
            //System.out.println("Service succesfully connected to the agent");
            //Request serviceDetails = new Request("service_started", 1);
            //serviceDetails.addEntry("service_type", serviceType);
            //serviceDetails.addEntry("service_socket", serviceSocket.getInetAddress().toString() + ":" + serviceSocket.getPort());
            //ObjectOutputStream managerOutput = new ObjectOutputStream(managerSocket.getOutputStream());
            //managerOutput.writeObject(serviceDetails);
            //managerOutput.flush();

        } catch (IOException e) {
            System.out.println("Error starting service: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }



}
