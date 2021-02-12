public class PublicKeyTransmission {

    private byte[] stream;
    private String ipAddress;
    public PublicKeyTransmission(){

    }
    public PublicKeyTransmission(byte[] stream) {
       this.stream = stream;
        try {
            this.ipAddress = Main.getIp();
        } catch (Exception e) {
            e.printStackTrace();
            Main.error("Failed to get public IP Address");
            System.exit(0);
        }
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public byte[] getStream() {
        return stream;
    }
}
