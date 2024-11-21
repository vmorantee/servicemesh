package dependencies;

public class ServiceInfo
{
    private String ipAddress;
    private String port;
    private int clientsNumber=0;

    public ServiceInfo(String ipAddress, String port)
    {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String getAddress()
    {
        return this.ipAddress;
    }

    public String getPort()
    {
        return this.port;
    }

    public void IncClientsNumber()
    {
        this.clientsNumber++;
    }

    public void RestartClientsNumber()
    {
        this.clientsNumber = 0;
    }

    public int getClientsNumber()
    {
        return this.clientsNumber;
    }
}
