import java.util.Vector;

public class APIAgent extends Agent{

    int agentIndex;
    public APIAgent(String ipAddress, String port, int index) {
        super(ipAddress, port);
        agentIndex = index;
    }

    @Override
    public String getAgentName() {
        return "APIAgent"+agentIndex;
    }

    @Override
    public Vector<String> fillPossibleServices() {
        Vector<String> possibleServices = new Vector<>();
        possibleServices.add("ApiGateway");
        return possibleServices;
    }
}
