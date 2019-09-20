import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class BackgroundInitializer {
    private String serverID;
    private byte[] signature;

    public BackgroundInitializer() {


    }
    public BackgroundInitializer(String serverID) {
        this.serverID = serverID;

        try {

            signature =  this.sign(serverID);
        } catch (Exception e) {
            Main.error("Unable to sign the server ID");
            System.exit(0);

        }


    }

    public byte[] sign(String data) throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException, SignatureException, IOException {
        Signature rsa = Signature.getInstance("SHA256withRSA");


        byte[] keyBytes = Files.readAllBytes(new File(Main.PRIVATE_KEY_FILE).toPath());
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey key = kf.generatePrivate(spec);
        rsa.initSign(key);
        rsa.update(data.getBytes());
        return rsa.sign();
    }

    /*
    Verify the integrity of a command sent from the server. Only commands properly signed by the command server that the client
    initially authenticated with will be executed.
     */

    public boolean verify() throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        byte[] keyBytes = Files.readAllBytes(new File(Main.PUBLIC_KEY_FILE).toPath());
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey key = kf.generatePublic(spec);


        sig.initVerify(key);
        sig.update(this.serverID.getBytes());

        return sig.verify(signature);
    }




}
