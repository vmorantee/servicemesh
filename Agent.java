import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Vector;

import static java.lang.Thread.sleep;

public abstract class Agent implements Runnable {
    private boolean isRunning = false;
    private String ipAddress, port;
    private HashMap<String, Socket> activeServices = new HashMap<>();
    private Vector<String> possibleServices = new Vector<>();
    private HashMap<Socket, Request> heartbeats = new HashMap<>();
    private Socket managerSocket;
    private ServerSocket agentSocket;

    public abstract String getAgentName();

    public Agent(String ipAddress, String port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String getPort() {
        return port;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void start() throws IOException {
        agentSocket = new ServerSocket(Integer.parseInt(this.getPort()));
        isRunning = true;
//        while (true) {
//            managerSocket = agentSocket.accept();
//            if (managerSocket != null) break;
//        }
        fillPossibleServices();
        new Thread(this).start();
        System.out.println("Agent started on port: " + this.getPort());
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

    public abstract void fillPossibleServices();

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
            if (possibleServices.contains(request.getRequestType())) {
                startServiceFromManager(request);
            } else if ("no_service_available".equals(request.getRequestType())) {
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
            ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", serviceType + ".jar");
            Process serviceProcess = processBuilder.start();
            System.out.println("Started service: " + serviceType);
            try{ //give time for the process to start
                sleep(15000);
            } catch (Exception e){

            }
            Socket serviceSocket = new Socket(serviceAddress, servicePort);
            System.out.println("Connected to service at: " + serviceAddress + ":" + servicePort);
            Request serviceDetails = new Request("service_started", 1);
            serviceDetails.addEntry("service_type", serviceType);
            serviceDetails.addEntry("service_socket", serviceSocket.getInetAddress().toString() + ":" + serviceSocket.getPort());
            ObjectOutputStream managerOutput = new ObjectOutputStream(managerSocket.getOutputStream());
            managerOutput.writeObject(serviceDetails);
            managerOutput.flush();

        } catch (IOException e) {
            System.out.println("Error starting service: " + e.getMessage());
        }
    }



}
