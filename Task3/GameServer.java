import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import models.ClientModel;
import models.QuestionModel;

public class GameServer {
    private static final int PORT = 5689;
    private static final int BUFFER_SIZE = 2048;
    private static final int MIN_PLAYERS = 2;
    private static DatagramSocket socket;
    private static final ConcurrentLinkedQueue<ClientModel> activeClients = new ConcurrentLinkedQueue<>();
    private static boolean gameInProgress = false;
    private static final int TOTAL_ROUNDS = 3;

    public static void main(String[] args) {
        try {
            socket = new DatagramSocket(PORT);
            System.out.println("Server is running on port " + PORT);

            while (true) {
                // Manage incoming client connections
                manageClientConnections();

                // Start the game if conditions are met
                if (!gameInProgress && activeClients.size() >= MIN_PLAYERS) {
                    System.out.println("Waiting 30 seconds for additional players...");
                    broadcastMessage("Waiting 30 seconds for additional players...");

                    // Allow new players to join during the waiting period
                    long waitEndTime = System.currentTimeMillis() + 30000;
                    while (System.currentTimeMillis() < waitEndTime) {
                        manageClientConnections(); // Handle new players joining
                    }

                    if (activeClients.size() >= MIN_PLAYERS) {
                        gameInProgress = true;
                        startGame();
                    } else {
                        System.out.println("Not enough players after wait. Waiting for more...");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void manageClientConnections() throws Exception {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, BUFFER_SIZE);
        socket.setSoTimeout(500);

        try {
            socket.receive(packet);

            String message = new String(packet.getData(), 0, packet.getLength()).trim();
            if (message.startsWith("JOIN-PLAYER:")) {
                String playerName = message.split(":")[1];
                ClientModel client = new ClientModel(playerName, packet.getAddress().toString(), packet.getPort());

                // Avoid duplicate joins
                if (!activeClients.stream().anyMatch(c -> c.getName().equals(playerName))) {
                    activeClients.add(client);
                    System.out.println("New player joined: " + playerName);

                    // Notify all players
                    broadcastMessage(playerName + " joined the lobby! Current players: " + activeClients.size());
                }
            }
        } catch (SocketTimeoutException e) {
            // No data received; continue listening
        }
    }

    private static void startGame() throws Exception {
        System.out.println("Game started!");
        broadcastMessage("Game started!");

        for (int round = 1; round <= TOTAL_ROUNDS; round++) {
            System.out.println("Starting round " + round + "...");
            broadcastMessage("Starting round " + round + "...");
            startRound();

            // Display scoreboard after each round
            displayScoreboard();
        }

        // Display final scoreboard and end game
        broadcastMessage("Game over! Final scoreboard:");
        displayScoreboard();
        gameInProgress = false;
    }

    private static void startRound() throws Exception {
        List<QuestionModel> questionList = new ArrayList<>(Arrays.asList(Questions.questions));
        Collections.shuffle(questionList);
        List<QuestionModel> roundQuestions = questionList.subList(0, 3);

        // Track which clients responded
        Set<ClientModel> respondingClients = new HashSet<>();

        for (QuestionModel question : roundQuestions) {
            Set<ClientModel> currentResponses = broadcastQuestion(question);
            respondingClients.addAll(currentResponses);
        }

        // Identify and remove non-responding clients only after the round
        Set<ClientModel> nonRespondingClients = new HashSet<>(activeClients);
        nonRespondingClients.removeAll(respondingClients);

        for (ClientModel client : nonRespondingClients) {
            activeClients.remove(client);
            System.out.println("Client " + client.getName() + " was removed for not responding.");
            broadcastMessage(client.getName() + " was removed for not responding.");
        }
    }

    private static void broadcastMessage(String message) {
        for (ClientModel client : activeClients) {
            try {
                sendPacket(message, client.getAddress(), client.getPort());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static Set<ClientModel> broadcastQuestion(QuestionModel question) throws Exception {
        System.out.println("Question: " + question.getQuestion() + ". Sent to the players. 30 seconds to answer.");
        broadcastMessage("Question: " + question.getQuestion() + ". You have 30 seconds to answer.");
        long endTime = System.currentTimeMillis() + 30000;
        Map<ClientModel, String> responses = new HashMap<>();

        while (System.currentTimeMillis() < endTime) {
            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.setSoTimeout((int) (endTime - System.currentTimeMillis()));

            try {
                socket.receive(packet);
                String response = new String(packet.getData(), 0, packet.getLength()).trim();
                InetAddress address = packet.getAddress();
                int port = packet.getPort();

                for (ClientModel client : activeClients) {
                    if (client.getAddress().equals(address.toString().replace("/", "")) && client.getPort() == port) {
                        responses.putIfAbsent(client, response);
                        break;
                    }
                }
            } catch (SocketTimeoutException e) {
                break;
            }
        }

        for (Map.Entry<ClientModel, String> entry : responses.entrySet()) {
            ClientModel client = entry.getKey();
            String answer = entry.getValue();
            if (answer.equalsIgnoreCase(question.getAnswer())) {
                client.incrementScore();
                sendPacket("Correct! Your score is now " + client.getScore(), client.getAddress(), client.getPort());
                System.out.println("Client " + client.getName() + " answered correctly.");
            } else {
                sendPacket("Incorrect! The correct answer was " + question.getAnswer(), client.getAddress(),
                        client.getPort());
                System.out.println("Client " + client.getName() + " answered incorrectly.");
            }
        }

        return responses.keySet();
    }

    private static void displayScoreboard() {
        StringBuilder scoreboard = new StringBuilder("Scoreboard:\n");
        for (ClientModel client : activeClients) {
            scoreboard.append(client.getName()).append(": ").append(client.getScore()).append("\n");
        }
        broadcastMessage(scoreboard.toString());
        System.out.println("Scoreboard:\n" + scoreboard.toString());
    }

    private static void sendPacket(String message, String address, int port) throws Exception {
        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(address), port);
        socket.send(packet);
    }
}