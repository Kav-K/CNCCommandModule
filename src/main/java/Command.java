import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class Command {
    private String command;
    private byte[] signature;

    public Command() {


    }

    public Command(String command) {
        this.command = command;
        try {
            this.signature = sign(command);
        } catch (Exception e) {
            e.printStackTrace();
            Main.error("Unable to digitally sign command: "+e.getMessage());
        }

    }

    public String getCommand() {
        return this.command;
    }

    public byte[] getSignature() {
        return signature;
    }

    /*
    Sign a command with a digital signature to be validated on the client side
     */

    public byte[] sign(String data) throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException, SignatureException, IOException {
        Signature rsa = Signature.getInstance("SHA1withRSA");


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
        Signature sig = Signature.getInstance("SHA1withRSA");
        byte[] keyBytes = Files.readAllBytes(new File(Main.PUBLIC_KEY_FILE).toPath());
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey key = kf.generatePublic(spec);


        sig.initVerify(key);
        sig.update(this.command.getBytes());

        return sig.verify(signature);
    }


}
