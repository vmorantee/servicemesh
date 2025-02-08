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

    public static void main(String[] args) {
        APIAgent apiAgent = new APIAgent("127.0.0.1","8086","127.0.0.1",8000,1,"127.0.0.1",8090);
        try {
            apiAgent.start(); //start agent
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
