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
        try {
            for (Connection c : Main.server.getConnections()) {
                if (Main.authenticatedClients.containsKey(c.getRemoteAddressTCP().getAddress().getHostAddress())) {
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
