package services;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import dependencies.Microservice;
import dependencies.Request;

public class LoginService extends Microservice implements Runnable {
    private boolean isRunning;
    private ServerSocket serverSocket;

    public static void main(String[] args)
    {
        if(args.length != 2)
            System.exit(100);

        LoginService l = new LoginService(args[0], args[1]);

        try {
            l.start();
        } catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    public LoginService(String ipAddress, String port) {
        super(ipAddress,port);
    }

    @Override
    public int handleMessage() {
        return 0;
    }

    public String getServiceName(){
        return "login";
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(Integer.parseInt(this.getPort()));
        isRunning = true;
        new Thread(this).start();
        System.out.println("LoginService started on port " + this.getPort());
    }


    public void stop() throws IOException {
        isRunning = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        System.out.println("LoginService stopped.");
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("LoginService accepted connection from " + clientSocket.getInetAddress());
                new Thread(() -> handleClientConnection(clientSocket)).start();
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
            Request request = (Request) objectInputStream.readObject();
            System.out.println("Received login request: " + request);
            Request response = new Request(request.getRequestType(), request.getRequestID());
            try (BufferedReader br = new BufferedReader(new FileReader("credentials.txt"))) {
                String line;
                String messageLogin = request.getContent("login").getEntryContent();
                String messagePassword = request.getContent("password").getEntryContent();
                boolean isFound=false;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(" ");
                    if (parts.length == 2) {
                        String login = parts[0];
                        String password = parts[1];
                        if(messageLogin.equals(login)&&messagePassword.equals(password)){
                            response.setMessageCode("200");
                            isFound=true;
                            break;
                        }
                    } else {
                        System.out.println("Invalid line format. Expected login and password.");
                    }
                }
                if(!isFound) response.setMessageCode("100");
            } catch (IOException e) {
                e.printStackTrace();
            }
            objectOutputStream.writeObject(response);
            objectOutputStream.flush();
            System.out.println("Login successful, response sent");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error handling client connection in LoginService: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing client socket in LoginService: " + e.getMessage());
            }
        }
    }
}
