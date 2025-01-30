import java.util.Vector;

public class APIAgent extends Agent{

    int agentIndex;
    public APIAgent(String agentIpAddress, String agentPort,
                    String managerIpAddress, int managerPort, int index,String apiAdress,int apiPort) {
        super(agentIpAddress, agentPort, managerIpAddress, managerPort);
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
