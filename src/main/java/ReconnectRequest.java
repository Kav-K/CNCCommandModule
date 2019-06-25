public class ReconnectRequest {
    private int timeout;

    public ReconnectRequest() {

    }

    public ReconnectRequest(int timeout) {
        this.timeout = timeout;
    }

    public int getTimeout() {
        return timeout;
    }

}
