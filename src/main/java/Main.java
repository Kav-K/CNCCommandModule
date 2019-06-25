import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.util.*;

public class Main {
    /**
     * @title CNCCommandModule
     * @author Kaveen Kumarasinghe
     * @date 06/24/2019
     * <p>
     * This is the counterpart of the CNCClientModule. This is a server that receives connections and sends commands to CNCClients (https://github.com/Kav-K/CNCClientModule/
     * This server will automatically bind to your external, front facing, and highest priority iPv4 address.
     * <p>
     * This project uses the KryoNet library from EsotericSoftware (https://github.com/EsotericSoftware/kryonet)
     * Please keep in mind that KryoNet is built upon Java NIO, and has a (mostly) asynchronous approach to message transmission and reception.
     */



    //Edit these parameters to change the maximum amount of data that can be received and/or transmitted.
    public static final int WRITEBUFFER = 50 * 1024;
    public static final int OBJECTBUFFER = 50 * 1024;

    static Server server;

    //Maps of <IP><Hostname> that have successfully authenticated with this command server.
    public static HashMap<String, String> authenticatedClients = new HashMap<String, String>();

    //List of classes to be registered with kryo for serialization/deserialization
    public static final List<Class> KRYO_CLASSES = Arrays.asList(AuthenticationConfirmation.class, Command.class, CommandResponse.class, KeepAlive.class, RegisterRequest.class);


    public static void main(String[] args) {
        initialize();
        registerClasses();
        startListener();
        startCommandResponseListener();
        startInputListener();


    }

    private static void initialize() {
        log("Starting server");
        server = new Server(WRITEBUFFER, OBJECTBUFFER);
        server.start();
        try {
            log("Binding to port 80");
            server.bind(80);
            log("Bound to port 80");

        } catch (Exception e) {
            e.printStackTrace();
            error("Could not bind to port and start server");
            System.exit(0);

        }


    }

    /*
    Listen for when a client sends a response to a command containing the command's output.
     */
    private static void startCommandResponseListener() {
        server.addListener(new Listener() {
            public void received(Connection connection, Object object) {
                if (object instanceof CommandResponse) {

                    CommandResponse commandResponse = (CommandResponse) object;
                    commandResponseLog(commandResponse.getResponseMessage(), connection.getRemoteAddressTCP().getAddress().getHostAddress(), authenticatedClients.get(connection.getRemoteAddressTCP().getAddress().getHostAddress()));


                }
            }

        });
    }


    private static void startInputListener() {
        Thread thread = new InputListener();
        thread.start();


    }


    /*
    The main listener for the command module. Listens for "RegisterRequests" which are requests for a client to be connected and controlled by this command module.
     */
    private static void startListener() {
        server.addListener(new Listener() {
            public void received(Connection connection, Object object) {
                if (object instanceof RegisterRequest) {
                    RegisterRequest request = (RegisterRequest) object;
                    System.out.println(connection.getRemoteAddressTCP().getAddress().getHostAddress());
                    System.out.println(request.getIpAddress());
                    if (request.getIpAddress().trim().equals(connection.getRemoteAddressTCP().getHostString().trim())) {

                        if (!authenticatedClients.containsKey(request.getIpAddress()))
                            authenticatedClients.put(request.getIpAddress(), request.getHostName());
                        log("Sending authentication confirmation");
                        connection.sendTCP(new AuthenticationConfirmation(request.getHostName(), request.getIpAddress(), connection.getID()));
                        log("Successfully authenticated: " + request.getHostName());


                        //Sends periodic keepalives to the client to ascertain the connection and to tell the client that command is still online.
                        Timer timer = new Timer("Timer");
                        TimerTask task = new TimerTask() {
                            public void run() {
                                connection.sendTCP(new KeepAlive());
                            }

                        };
                        timer.scheduleAtFixedRate(task, 1000, 3000);


                    } else {
                        error("Invalid authentication request was sent by: " + request.getHostName());
                        connection.close();
                    }

                }
            }
        });


    }


    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static void log(String message) {
        System.out.println(ANSI_GREEN + "[Command] " + ANSI_RESET + message);
    }

    public static void error(String message) {
        System.out.println(ANSI_RED + "[Command] " + ANSI_RESET + message);
    }

    public static void commandLog(String message, String host, String hostname) {
        System.out.println(ANSI_BLUE + "[COMMAND SENT][" + host + "] [" + hostname + "] " + ANSI_RESET + message);
    }

    public static void commandResponseLog(String message, String host, String hostname) {
        System.out.println(ANSI_YELLOW + "[COMMAND RESPONSE][" + host + "] [" + hostname + "] " + ANSI_RESET + message);
    }

    private static void registerClasses() {
        Kryo kryo = server.getKryo();
        log("Registering classes with kryo for (de)serialization");
        for (Class c : KRYO_CLASSES) kryo.register(c);
        log("Successfully registered classes with kryo");

    }
}
