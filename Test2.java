public class Test2 {
    public static void main(String[] args) {
        ServiceAgent ServiceAgent = new ServiceAgent("127.0.0.1","8100","127.0.0.1",8000,1);
        try {
            ServiceAgent.start(); //start agent
            Request request = new Request("FakeRequest", 1558);
            request.addEntry("service_type", "Register");
            request.addEntry("service_address", "127.0.0.1");
            request.addEntry("service_port", "8090");
            ServiceAgent.startServiceFromManager(request); //manual test of agent
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
