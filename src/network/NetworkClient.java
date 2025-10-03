package network;

import core.PlayerState;
import java.awt.Point;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkClient {
    private static final String SERVER_IP = "localhost";
    private static final int PORT = 12345;
    
    private PlayerData myPlayerData;
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    
    private Map<String, OnlinePlayer> onlinePlayers = new ConcurrentHashMap<>();
    private boolean isConnected = false;
    
    public NetworkClient(String playerId, String playerName, Point startPosition, String characterImage) {
        this.myPlayerData = new PlayerData(playerId, playerName, startPosition, characterImage);
    }
    
    public NetworkClient(PlayerData playerData) {
        this.myPlayerData = playerData.copy();
    }
    
    public void connect() {
        new Thread(() -> {
            try {
                clientSocket = new Socket(SERVER_IP, PORT);
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                in = new ObjectInputStream(clientSocket.getInputStream());
                
                isConnected = true;
                System.out.println("Connected to server");
                
                sendMessage(NetworkMessage.createPlayerJoin(myPlayerData));
                
                while (isConnected && !clientSocket.isClosed()) {
                    NetworkMessage msg = (NetworkMessage) in.readObject();
                    handleMessage(msg);
                }
            } catch (Exception e) {
                System.out.println("Connection error: " + e.getMessage());
                isConnected = false;
                showConnectionError();
            }
        }).start();
    }
    
    private void handleMessage(NetworkMessage msg) {
        switch (msg.type) {
            case PLAYER_JOIN:
                if (!msg.playerData.playerId.equals(myPlayerData.playerId)) {
                    OnlinePlayer newPlayer = new OnlinePlayer(msg.playerData);
                    onlinePlayers.put(msg.playerData.playerId, newPlayer);
                    System.out.println("Player joined: " + msg.playerData.playerName + " at " + msg.playerData.position);
                }
                break;
                
            case PLAYER_MOVE:
                if (!msg.playerData.playerId.equals(myPlayerData.playerId)) {
                    OnlinePlayer player = onlinePlayers.get(msg.playerData.playerId);
                    if (player != null) {
                        player.updatePosition(msg.playerData.position);
                        System.out.println("Player moved: " + msg.playerData.playerId + " to " + msg.playerData.position);
                    }
                }
                break;
                
            case PLAYER_UPDATE:
                if (!msg.playerData.playerId.equals(myPlayerData.playerId)) {
                    OnlinePlayer player = onlinePlayers.get(msg.playerData.playerId);
                    if (player != null) {
                        player.updateFromPlayerData(msg.playerData);
                        System.out.println("Player updated: " + msg.playerData.playerId);
                    }
                }
                break;
                
            case PLAYER_STATS_UPDATE:
                if (!msg.playerData.playerId.equals(myPlayerData.playerId)) {
                    OnlinePlayer player = onlinePlayers.get(msg.playerData.playerId);
                    if (player != null) {
                        player.updateStats(msg.playerData.money, msg.playerData.health, msg.playerData.energy);
                        System.out.println("Player stats updated: " + msg.playerData.playerId);
                    }
                }
                break;
                
            case PLAYER_LOCATION_CHANGE:
                if (!msg.playerData.playerId.equals(myPlayerData.playerId)) {
                    OnlinePlayer player = onlinePlayers.get(msg.playerData.playerId);
                    if (player != null) {
                        player.updateLocation(msg.playerData.currentLocation);
                        System.out.println("Player location changed: " + msg.playerData.playerId + " to " + msg.playerData.currentLocation);
                    }
                }
                break;
                
            case PLAYER_LEAVE:
                onlinePlayers.remove(msg.playerData.playerId);
                System.out.println("Player left: " + msg.playerData.playerId);
                break;
        }
    }
    
    public void sendPlayerMove(Point newPosition) {
        myPlayerData.updatePosition(newPosition);
        if (isConnected) {
            sendMessage(NetworkMessage.createPlayerMove(myPlayerData.playerId, newPosition));
        }
    }
    
    public void sendPlayerStatsUpdate(int money, int health, int energy) {
        myPlayerData.updateStats(money, health, energy);
        if (isConnected) {
            sendMessage(NetworkMessage.createPlayerStatsUpdate(myPlayerData.playerId, money, health, energy));
        }
    }
    
    public void sendPlayerLocationChange(PlayerState.Location location) {
        myPlayerData.updateLocation(location);
        if (isConnected) {
            sendMessage(NetworkMessage.createPlayerLocationChange(myPlayerData.playerId, location));
        }
    }
    
    public void sendPlayerUpdate() {
        if (isConnected) {
            sendMessage(NetworkMessage.createPlayerUpdate(myPlayerData));
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
    
    public PlayerData getMyPlayerData() {
        return myPlayerData.copy();
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
    
    private void showConnectionError() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JOptionPane optionPane = new javax.swing.JOptionPane(
                "Server connection error\n\n" +
                "Game will close in 3 seconds...", 
                javax.swing.JOptionPane.ERROR_MESSAGE);
            optionPane.setFont(util.FontManager.getFontForText("Server connection error", 14));
            
            javax.swing.JDialog dialog = optionPane.createDialog(null, "Connection Error");
            dialog.setFont(util.FontManager.getFontForText("Connection Error", 14));
            dialog.setVisible(true);
            
            new javax.swing.Timer(3000, e -> {
                System.exit(0);
            }).start();
        });
    }
}
