import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class GameClient {
    private static volatile boolean gameStarted = false;
    private static volatile boolean waitingForInput = false;

    public static void main(String[] args) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        byte[] responseBuffer = new byte[1024];
        DatagramSocket socket = null;

        try {
            // Get the server IP
            System.out.println("Welcome! Please enter the server IP address (e.g., localhost):");
            String serverAddress = reader.readLine().trim();

            // Get the server port
            System.out.println("Please enter the server port (e.g., 5689):");
            int serverPort;
            while (true) {
                try {
                    serverPort = Integer.parseInt(reader.readLine().trim());
                    break;
                } catch (NumberFormatException e) {
                    System.out.println("Invalid port number. Please enter a valid port:");
                }
            }

            // Get the user name
            System.out.println("Please enter your name:");
            String name = reader.readLine().trim();
            while (name.isEmpty()) {
                System.out.println("Name cannot be empty. Please enter your name:");
                name = reader.readLine().trim();
            }

            // Create the socket
            socket = new DatagramSocket();
            InetAddress serverInetAddress = InetAddress.getByName(serverAddress);

            // Send join message
            String playerJoinMessage = "JOIN-PLAYER:" + name;
            byte[] buffer = playerJoinMessage.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverInetAddress, serverPort);
            socket.send(packet);
            System.out.println("Connected to server at " + serverAddress + ":" + serverPort);
            System.out.println("Message sent to the server: " + playerJoinMessage);

            // Start the loading animation in a separate thread
            Thread loadingThread = new Thread(() -> displayLoadingAnimation("Waiting for the game to start..."));
            loadingThread.start();

            String response = "";
            String previousResponse = "";

            while (true) {
                // Wait for a response from the server
                DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
                socket.receive(responsePacket);

                response = new String(responsePacket.getData(), 0, responsePacket.getLength());
                if (!response.equals(previousResponse)) {
                    System.out.println(response);
                    previousResponse = response;

                    if (response.startsWith("Game over!")) {
                        System.out.println("Thank you for playing! Disconnecting...");
                        break;
                    }

                    if (response.startsWith("Question:") && !waitingForInput) {
                        // Stop the loading animation once a response is received
                        gameStarted = true;

                        // Handle user input with timeout
                        waitingForInput = true;
                        String answer = getUserInputWithTimeout(reader, 30);
                        if (answer == null) {
                            System.out.println("Time's up! Moving to the next question.");
                        } else {
                            buffer = answer.getBytes();
                            packet = new DatagramPacket(buffer, buffer.length, serverInetAddress, serverPort);
                            socket.send(packet);
                            System.out.println("Answer sent to server.");
                        }
                        // Ensure flag reset even if timeout occurs or input is given
                        waitingForInput = false;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close the socket
            if (socket != null) {
                socket.close();
            }
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void displayLoadingAnimation(String message) {
        String[] spinner = { "|", "/", "-", "\\" };
        int spinnerIndex = 0;

        while (!gameStarted) {
            System.out.print("\r" + message + " " + spinner[spinnerIndex]);
            System.out.flush();
            spinnerIndex = (spinnerIndex + 1) % spinner.length;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.print("\r" + " ".repeat(message.length() + 2) + "\r");
        System.out.flush();
    }

    private static String getUserInputWithTimeout(BufferedReader reader, int timeoutInSeconds) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = null;

        try {
            // Submit the input task
            future = executor.submit(() -> {
                try {
                    return reader.readLine();
                } catch (IOException e) {
                    return null;
                }
            });
            
            // Wait for input or timeout
            String answer = future.get(timeoutInSeconds, TimeUnit.SECONDS);

            // Return the answer if received
            return answer;

        } catch (TimeoutException e) {
            System.out.println("Timeout reached, no input received.");
            return null; // Return null if timeout occurs
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            // Shutdown the executor after the task completes
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
            executor.shutdownNow();
        }
    }
}