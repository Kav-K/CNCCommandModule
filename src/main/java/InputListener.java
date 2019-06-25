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
        if (Main.server.getConnections().length <1) {
            Main.error("There are no active client connections to dispatch commands to");
            start();


        }


        try {
            for (Connection c : Main.server.getConnections()) {
                if (Main.authenticatedClients.containsKey(c.getRemoteAddressTCP().getAddress().getHostAddress())) {
                    if (input.equals("_KILL_")) {
                        c.sendTCP(new KillRequest(false));
                        Main.commandLog("--SENT KILL REQUEST--",c.getRemoteAddressTCP().getAddress().getHostAddress(),Main.authenticatedClients.get(c.getRemoteAddressTCP().getAddress().getHostAddress()));
                        Main.authenticatedClients.remove(c.getRemoteAddressTCP().getAddress().getHostAddress());
                        continue;
                    }


                    c.sendTCP(new Command(input));

                    Main.commandLog(input,c.getRemoteAddressTCP().getAddress().getHostAddress(),Main.authenticatedClients.get(c.getRemoteAddressTCP().getAddress().getHostAddress()));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        start();


    }


}
