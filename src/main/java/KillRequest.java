public class KillRequest {
    /*
    If boolean destroy is set to true in the killrequest, it will also delete the current version of the client from the filesystem.
    This makes it easy to update clients.
     */

    public boolean destroy = false;

    public KillRequest() {

    }
    
    public KillRequest(boolean destroy) {
        this.destroy = destroy;
    }

}
