import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Main {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    static Server server;
    public static ArrayList<String> authenticatedIps = new ArrayList<String>();





    public static void main(String[] args) {
        initialize();
        registerClasses();
        startListener();
        startCommandResponseListener();
        startInputListener();




    }

    private static void startCommandResponseListener() {
        server.addListener(new Listener() {
            public void received(Connection connection, Object object) {
                if (object instanceof CommandResponse) {

                    CommandResponse commandResponse = (CommandResponse) object;
                    commandResponseLog(commandResponse.getResponseMessage(), connection.getRemoteAddressTCP().getAddress().getHostAddress());


                }
            }

        });
    }


    private static void startInputListener() {
        Thread thread = new InputListener();
        thread.start();


    }
    private static void registerClasses() {
        Kryo kryo = server.getKryo();
        kryo.register(RegisterRequest.class);
        kryo.register(AuthenticationConfirmation.class);
        kryo.register(KeepAlive.class);
        kryo.register(Command.class);
        kryo.register(CommandResponse.class);

    }



   private static void startListener() {
       server.addListener(new Listener() {
           public void received (Connection connection, Object object) {
               if (object instanceof RegisterRequest) {
                   RegisterRequest request = (RegisterRequest) object;
                   System.out.println(connection.getRemoteAddressTCP().getAddress().getHostAddress());
                   System.out.println(request.getIpAddress());
                   if (request.getIpAddress().trim().equals(connection.getRemoteAddressTCP().getHostString().trim())) {

                           if (!authenticatedIps.contains(request.getIpAddress())) authenticatedIps.add(request.getIpAddress());
                           log("Sending authentication confirmation");
                           connection.sendTCP(new AuthenticationConfirmation(request.getHostName(),request.getIpAddress(),connection.getID()));
                           log("Successfully authenticated: "+request.getHostName());


                           //TODO Send keep alives to the client periodically, make client check for them and set unauthenticated if they're not there after x time

                           Timer timer = new Timer("Timer");
                           TimerTask task = new TimerTask() {
                               public void run() {
                                  connection.sendTCP(new KeepAlive());
                               }

                           };
                           timer.scheduleAtFixedRate(task,1000,3000);






                   } else {
                       error("Invalid authentication request was sent by: "+request.getHostName());
                       connection.close();
                   }

               }
           }
       });



   }

    private static void initialize() {
        log("Starting server");
        server = new Server();
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


    public static void log(String message){
        System.out.println(ANSI_GREEN+"[Command] "+ANSI_RESET+message);
    }
    public static void error(String message) {
        System.out.println(ANSI_RED+"[Command] "+ANSI_RESET+message);
    }
    public static void commandLog(String message, String host)  {
        System.out.println(ANSI_BLUE+"[COMMAND SENT]["+host+"] "+ANSI_RESET+message);
    }

    public static void commandResponseLog(String message, String host) {
        System.out.println(ANSI_YELLOW + "[COMMAND RESPONSE][" + host + "] " + ANSI_RESET + message);
    }
}
