import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.util.ArrayList;

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



    }
    private static void registerClasses() {
        Kryo kryo = server.getKryo();
        kryo.register(RegisterRequest.class);

    }

   private static void startListener() {
       server.addListener(new Listener() {
           public void received (Connection connection, Object object) {
               if (object instanceof RegisterRequest) {
                   RegisterRequest request = (RegisterRequest) object;
                   if (request.getIpAddress().trim().equals(connection.getRemoteAddressTCP().getHostString().trim())) {
                       if (request.getHostName().trim().equals(connection.getRemoteAddressTCP().getHostName().trim())) {
                           authenticatedIps.add(request.getIpAddress());
                           connection.sendTCP(new AuthenticationConfirmation(request.getHostName(),request.getIpAddress(),connection.getID()));




                       } else {
                           connection.close();
                       }
                   } else {
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
}
