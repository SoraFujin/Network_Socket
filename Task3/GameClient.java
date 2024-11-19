import java.net.*;

public class GameClient {
    public static void main(String[] args) {
        String serverAddress = "127.0.0.1";
        final int serverPort = 5689;
        String message = "JOIN";

        DatagramSocket socket = null;

        try {
            // Step 1: Create the socket
            socket = new DatagramSocket();

            // Step 2: Convert message to bytes
            byte[] buffer = message.getBytes();

            // Step 3: Create the DatagramPacket
            InetAddress serverInetAddress = InetAddress.getByName(serverAddress);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverInetAddress, serverPort);

            // Step 4: Send the packet
            socket.send(packet);
            System.out.println("Message sent to the server: " + message);

            // Optionally, you can wait for a response from the server
            byte[] responseBuffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            socket.receive(responsePacket);

            String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
            System.out.println("Response from server: " + response);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close the socket
            if (socket != null) {
                socket.close();
            }
        }
    }
}
