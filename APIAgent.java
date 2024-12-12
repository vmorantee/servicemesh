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
    public void fillPossibleServices() {

    }
}
