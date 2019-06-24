public class AuthenticationConfirmation {
    public static String message = "Successfully authenticated";
    private int id;
    private String hostname;
    private String ipAddress;
    public AuthenticationConfirmation(){

    }
    public AuthenticationConfirmation(String hostname, String ipAddress, int id) {
        this.hostname = hostname;
        this.ipAddress = ipAddress;
        this.id = id;

    }

    public String getHostname(){
        return this.hostname;
    }
    public int getId(){
        return this.id;
    }
    public String getIpAddress(){
        return this.ipAddress;
    }
}
