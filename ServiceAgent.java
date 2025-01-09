import java.util.Vector;

public class ServiceAgent extends Agent{

    int agentIndex;
    public ServiceAgent(String ipAddress, String port, int index) {
        super(ipAddress, port);
        agentIndex = index;
    }

    @Override
    public String getAgentName() {
        return "ServiceAgent"+agentIndex;
    }

    @Override
    public Vector<String> fillPossibleServices() {
        Vector<String> possibleServices = new Vector<>();
        possibleServices.add("Login");
        possibleServices.add("Register");
        possibleServices.add("FileUpload");
        return possibleServices;
    }
}
