package network;

import java.awt.Point;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkClient {
    private static final String SERVER_IP = "localhost";
    private static final int PORT = 12345;
    
    private String myPlayerId;
    private String myPlayerName;
    private Point myPosition;
    private String myCharacterImage;
    
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    
    private Map<String, OnlinePlayer> onlinePlayers = new ConcurrentHashMap<>();
    private boolean isConnected = false;
    
    public NetworkClient(String playerId, String playerName, Point startPosition, String characterImage) {
        this.myPlayerId = playerId;
        this.myPlayerName = playerName;
        this.myPosition = startPosition;
        this.myCharacterImage = characterImage;
    }
    
    public void connect() {
        new Thread(() -> {
            try {
                clientSocket = new Socket(SERVER_IP, PORT);
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                in = new ObjectInputStream(clientSocket.getInputStream());
                
                isConnected = true;
                System.out.println("Connected to server");
                
                sendMessage(NetworkMessage.createPlayerJoin(myPlayerId, myPlayerName, myPosition, myCharacterImage));
                
                while (isConnected && !clientSocket.isClosed()) {
                    NetworkMessage msg = (NetworkMessage) in.readObject();
                    handleMessage(msg);
                }
            } catch (Exception e) {
                System.out.println("Connection error: " + e.getMessage());
                isConnected = false;
            }
        }).start();
    }
    
    private void handleMessage(NetworkMessage msg) {
        switch (msg.type) {
            case PLAYER_JOIN:
                if (!msg.playerId.equals(myPlayerId)) {
                    OnlinePlayer newPlayer = new OnlinePlayer(msg.playerId, msg.playerName, msg.position, msg.characterImage);
                    onlinePlayers.put(msg.playerId, newPlayer);
                    System.out.println("Player joined: " + msg.playerName + " at " + msg.position + " with " + msg.characterImage);
                }
                break;
                
            case PLAYER_MOVE:
                if (!msg.playerId.equals(myPlayerId)) {
                    OnlinePlayer player = onlinePlayers.get(msg.playerId);
                    if (player != null) {
                        player.updatePosition(msg.position);
                        System.out.println("Player moved: " + msg.playerId + " to " + msg.position);
                    }
                }
                break;
                
            case PLAYER_LEAVE:
                onlinePlayers.remove(msg.playerId);
                System.out.println("Player left: " + msg.playerId);
                break;
        }
    }
    
    public void sendPlayerMove(Point newPosition) {
        myPosition = newPosition;
        if (isConnected) {
            sendMessage(NetworkMessage.createPlayerMove(myPlayerId, newPosition));
        }
    }
    
    private void sendMessage(NetworkMessage msg) {
        try {
            if (out != null) {
                out.writeObject(msg);
            }
        } catch (IOException e) {
            System.out.println("Send error: " + e.getMessage());
        }
    }
    
    public Map<String, OnlinePlayer> getOnlinePlayers() {
        return onlinePlayers;
    }
    
    public boolean isConnected() {
        return isConnected;
    }
    
    public void disconnect() {
        isConnected = false;
        try {
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Disconnect error: " + e.getMessage());
        }
    }
}
