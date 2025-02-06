import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;

public class CLI {
    private Scanner scanner;
    private String apiIP, apiPort;
    private boolean isLogged=false;
    private int reqID=1;
    private String login=null;
    private Socket connection;
    private ObjectInputStream inputStream = null;
    private ObjectOutputStream outputStream = null;
    public CLI(String apiIP,String apiPort) {
        this.apiIP=apiIP;
        this.apiPort=apiPort;
        this.scanner = new Scanner(System.in);
    }
    public void registrationLogic(){
        try{
            if(inputStream == null && outputStream == null)
            {
                try
                {
                    outputStream = new ObjectOutputStream(connection.getOutputStream());
                    inputStream = new ObjectInputStream(connection.getInputStream());
                } catch(Exception e)
                {
                    System.out.println(e);
                }
            }
            System.out.print("Enter username: ");
            String username = scanner.nextLine();
            System.out.print("Enter password: ");
            String password = scanner.nextLine();
            Request loginRequest = new Request("service_request", reqID++);
            loginRequest.addEntry("service_type", "RegistrationService");
            loginRequest.addEntry("login", username);
            loginRequest.addEntry("password", password);
            outputStream.writeObject(loginRequest);
            outputStream.flush();
            Request response = (Request) inputStream.readObject();
            if ("200".equals(response.getMessageCode())) {
                System.out.println("Registration successful!");
                isLogged=true;
            } else {
                System.out.println("Registration failed: " + response.getMessageCode());
            }
        }catch (IOException | ClassNotFoundException e) {
            System.out.println("Error connecting to API Gateway: " + e.getMessage());
        }
    }
    public void start() {
        try{
            connection = new Socket(apiIP,Integer.parseInt(apiPort));
        }catch (Exception e){
            System.out.println("Error " + e);
        }
        if (connection != null) {
            while (true) {
                System.out.println("Welcome to the CLI!\n1-Login\n2-Register");
                if(isLogged)
                    System.out.println("3-Upload a file");
                String option = scanner.nextLine();
                switch (option) {
                    case "1":
                        loginLogic();
                        break;
                    case "2":
                        registrationLogic();
                    case "3":
                        uploadLogic();
                        break;


                }
            }
        }

    }
    public void uploadLogic(){
        if (login != null) {
            if(inputStream == null && outputStream == null)
            {
                try
                {
                    outputStream = new ObjectOutputStream(connection.getOutputStream());
                    inputStream = new ObjectInputStream(connection.getInputStream());
                } catch(Exception e)
                {
                    System.out.println(e);
                }
            }
            System.out.println("Write the name of the file you want to upload:");
            String filename = scanner.nextLine();
            File file = new File(filename);
            if (file.exists()) {
                try (
                        FileInputStream fis = new FileInputStream(file);
                        BufferedInputStream bis = new BufferedInputStream(fis)
                ) {
                    long fileSize = file.length();
                    int chunkSize = 512;
                    byte[] buffer = new byte[chunkSize];
                    int bytesRead;
                    int currentPackage = 1;
                    int totalPackages = (int) Math.ceil((double) fileSize / chunkSize);
                    System.out.println("Beginning to send file in " + totalPackages + " packages...");

                    while ((bytesRead = bis.read(buffer, 0, buffer.length)) != -1) {
                        Request request = new Request("file_upload", reqID++);
                        request.addEntry("service_type", "FileUploadService");
                        request.addEntry("username", login);
                        request.addEntry("filename", filename);
                        request.addEntry("total_packages", Integer.toString(totalPackages));
                        request.addEntry("current_package_number", Integer.toString(currentPackage));
                        request.addEntry("data", Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, bytesRead)));
                        outputStream.writeObject(request);
                        outputStream.flush();
                        Request ack = (Request) inputStream.readObject();
                        if (!"ack".equals(ack.getRequestType())) {
                            System.out.println("Error: No acknowledgment for package " + currentPackage);
                            break;
                        }
                        System.out.println("Sent package " + currentPackage + " of " + totalPackages);
                        currentPackage++;
                    }
                } catch (Exception e) {
                    System.out.println("Error uploading file: " + e.getMessage());
                }
            } else {
                System.out.println("File does not exist.");
            }
        } else {
            System.out.println("Only logged-in users can upload files.");
        }
    }
    public void loginLogic(){
        try {
            if(inputStream == null && outputStream == null)
            {
                try
                {
                    outputStream = new ObjectOutputStream(connection.getOutputStream());
                    inputStream = new ObjectInputStream(connection.getInputStream());
                } catch(Exception e)
                {
                    System.out.println(e);
                }
            }
            System.out.print("Enter username: ");
            String username = scanner.nextLine();
            System.out.print("Enter password: ");
            String password = scanner.nextLine();
            Request loginRequest = new Request("Login", reqID++);
            loginRequest.addEntry("service_type", "LoginService");
            loginRequest.addEntry("login", username);
            loginRequest.addEntry("password", password);
            outputStream.writeObject(loginRequest);
            outputStream.flush();
            Request response = (Request) inputStream.readObject();
            if ("200".equals(response.getMessageCode())) {
                System.out.println("Login successful!");
                isLogged=true;
                this.login=username;
            } else {
                System.out.println("Login failed: " + response.getMessageCode());
            }
        }catch (IOException | ClassNotFoundException e) {
            System.out.println("Error connecting to API Gateway: " + e.getMessage());
        }
    }
    public static void main(String[] args) {
        RegistrationService r = new RegistrationService("127.0.0.1","8082");
        FileUploadService fus = new FileUploadService("127.0.0.1","8083");
        try {
            r.start();
            fus.start();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(fus);
        CLI cli = new CLI("127.0.0.1","8090");
        cli.start();
    }
}
