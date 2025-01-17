public class Test {
    public static void main(String[] args) {
        APIAgent apiAgent = new APIAgent("127.0.0.1","8086",1);
        try {
            apiAgent.start(); //start agent
            Request request = new Request("FakeRequest", 1558);
            request.addEntry("service_type", "ApiGateway");
            request.addEntry("service_address", "127.0.0.1");
            request.addEntry("service_port", "8088");
            apiAgent.startServiceFromManager(request); //manual test of agent
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
