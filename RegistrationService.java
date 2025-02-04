import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class RegistrationService extends Microservice implements Runnable {
    private boolean isRunning;
    private ServerSocket serverSocket;

    public RegistrationService(String ipAddress, String port) {
        super(ipAddress,port);
    }

    @Override
    public int handleMessage() {
        return 0;
    }

    public String getServiceName(){
        return "register";
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(Integer.parseInt(this.getPort()));
        isRunning = true;
        new Thread(this).start();
        System.out.println("RegistrationService started on port " + this.getPort());
    }


    public void stop() throws IOException {
        isRunning = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        System.out.println("RegistrationService stopped.");
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                System.out.println("RegistrationService listening on port " + this.getPort());
                Socket clientSocket = serverSocket.accept();
                System.out.println("RegistrationService accepted connection from " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

                new Thread(() -> handleClientConnection(clientSocket)).start();
            } catch (IOException e) {
                if (isRunning) {
                    System.out.println("Error accepting connection: " + e.getMessage());
                }
            }
        }
    }

    private void handleClientConnection(Socket clientSocket) {
        try (
                ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream())
        ) {
            System.out.println("Registration: Awaiting request");
            Request request = (Request) objectInputStream.readObject();
            System.out.println("Received registration request: " + request);
            Request response = new Request(request.getRequestType(), request.getRequestID());
            File f = new File("credentials.txt");
            boolean doesExist=false;
            try (BufferedReader br = new BufferedReader(new FileReader("credentials.txt"))) {
                String line;
                String messageLogin = request.getContent("login").getEntryContent();
                String messagePassword = request.getContent("password").getEntryContent();
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(" ");
                    if (parts.length == 2) {
                        String login = parts[0];
                        if(messageLogin.equals(login)){
                            response.setMessageCode("100");
                            System.out.println("Account like this exist");
                            doesExist=true;
                            break;
                        }
                    }
                }
                if(!doesExist){
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter("credentials.txt", true))) {
                        writer.newLine();
                        writer.write(messageLogin+" "+messagePassword);
                        response.setMessageCode("200");
                        System.out.println("Account added successfully");
                    } catch (IOException e) {
                        System.out.println( e.getMessage());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            objectOutputStream.writeObject(response);
            objectOutputStream.flush();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error handling client connection in RegistrationService: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing client socket in RegistrationService: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        RegistrationService rs = new RegistrationService(args[0],args[1]);
        try {
            rs.start();
        } catch (Exception e){
            System.out.println(e);
        }
    }
}
