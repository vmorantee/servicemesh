import java.util.ArrayList;

public abstract class Microservice {
    private String ipAddress;
    private String port;
    public abstract String getServiceName();
    private static final ArrayList<Microservice> instances = new ArrayList<>();
    public Microservice(String ipAddress, String port){
        this.ipAddress = ipAddress;
        this.port = port;
        instances.add(this);
    }
    public static ArrayList<Microservice> getInstances(){
        return new ArrayList<>(instances);
    }
    public String getIpAddress(){
        return this.ipAddress;
    }
    public String getPort(){
        return this.port;
    }
    public abstract int handleMessage();
}
