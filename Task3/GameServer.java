import java.net.*;
import java.io.*;

public class GameServer {
    public static void main(String[] args) {
        final int port = 5689;
        byte[] buffer = new byte[1024];
        DatagramSocket socket = null;

        try {
            // Create the socket
            socket = new DatagramSocket(port);
            System.out.println("Server is listening on port " + port);

            InetAddress player1Address = null;
            int player1Port = 0;
            InetAddress player2Address = null;
            int player2Port = 0;

            while (true) {
                // Receive a packet
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData());
                System.out.println("Received: " + message + " from " + packet.getAddress() + ":" + packet.getPort());

                // Register players
                if (message.startsWith("JOIN")) {
                    if (player1Address == null) {
                        player1Address = packet.getAddress();
                        player1Port = packet.getPort();
                        System.out.println("Player 1 joined");
                    } else if (player2Address == null) {
                        player2Address = packet.getAddress();
                        player2Port = packet.getPort();
                        System.out.println("Player 2 joined");
                    } else {
                        System.out.println("Game is full.");
                    }
                }

                // Handle game messages
                if (player1Address != null && player2Address != null) {
                    InetAddress targetAddress = packet.getAddress().equals(player1Address) ? player2Address : player1Address;
                    int targetPort = packet.getPort() == player1Port ? player2Port : player1Port;

                    // Forward the message to the other player
                    DatagramPacket forwardPacket = new DatagramPacket(
                        message.getBytes(),
                        message.length(),
                        targetAddress,
                        targetPort
                    );
                    socket.send(forwardPacket);
                    System.out.println("Forwarded message to " + targetAddress + ":" + targetPort);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}
