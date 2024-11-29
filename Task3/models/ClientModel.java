package models;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ClientModel {
    private String name;
    private String address;
    private int port;
    private int score;
    private boolean canAnswer;

    public ClientModel(String name, String address, int port) {
        this.name = name;
        this.address = address;
        this.port = port;
        this.score = 0;
        this.canAnswer = true;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        if (address.startsWith("/")) {
            return address.substring(1);
        }
        return address;
    }

    public InetAddress getInetAddress()  {
        try {
            return InetAddress.getByName(getAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getPort() {
        return port;
    }

    public int getScore() {
        return score;
    }

    public void incrementScore() {
        score++;
    }

    public boolean canAnswer() {
        return canAnswer;
    }

    public void setCanAnswer(boolean canAnswer) {
        this.canAnswer = canAnswer;
    }

    @Override
    public String toString() {
        return name + " (" + address + ":" + port + ")";
    }
}