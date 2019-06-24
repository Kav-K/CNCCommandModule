public class RegisterRequest {
    private String hostName;
    private String ipAddress;

    public RegisterRequest() {

    }
    public RegisterRequest(String hostName, String ipAddress) {
        this.hostName = hostName;
        this.ipAddress = ipAddress;

    }

    public String getHostName() {
        return this.hostName;

    }
    public String getIpAddress(){
        return this.ipAddress;
    }


}
