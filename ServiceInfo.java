public class ServiceInfo
{
    private String ipAddress;
    private String port;

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
}
