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
        System.out.println("NetworkClient created for: " + playerName + " with character: " + characterImage);
    }
    
    public NetworkClient(PlayerData playerData) {
        this.myPlayerData = playerData.copy();
    }
    
    public void connect() {
        new Thread(() -> {
            int retryCount = 0;
            int maxRetries = 3;
            
            while (retryCount < maxRetries && !isConnected) {
                try {
                    clientSocket = new Socket(SERVER_IP, PORT);
                    clientSocket.setSoTimeout(5000);
                    out = new ObjectOutputStream(clientSocket.getOutputStream());
                    in = new ObjectInputStream(clientSocket.getInputStream());
                    
                    isConnected = true;
                    System.out.println("Connected to server");
                    
                    Thread.sleep(100);
                    sendMessage(NetworkMessage.createPlayerJoin(myPlayerData));
                    
                    while (isConnected && !clientSocket.isClosed()) {
                        try {
                            NetworkMessage msg = (NetworkMessage) in.readObject();
                            handleMessage(msg);
                        } catch (java.net.SocketTimeoutException e) {
                            continue;
                        } catch (java.io.EOFException e) {
                            break;
                        } catch (java.net.SocketException e) {
                            break;
                        }
                    }
                    break;
                } catch (Exception e) {
                    retryCount++;
                    System.out.println("Connection attempt " + retryCount + " failed: " + e.getMessage());
                    
                    try {
                        if (clientSocket != null && !clientSocket.isClosed()) {
                            clientSocket.close();
                        }
                    } catch (IOException closeE) {
                        System.out.println("Close error: " + closeE.getMessage());
                    }
                    
                    if (retryCount < maxRetries) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                            break;
                        }
                    }
                }
            }
            
            if (!isConnected) {
                System.out.println("Failed to connect after " + maxRetries + " attempts");
                showConnectionError();
            }
        }).start();
    }
    
    private OnlineDataManager dataManager = new OnlineDataManagerImpl();
    
    private void handleMessage(NetworkMessage msg) {
        if (msg == null || msg.playerData == null) {
            System.out.println("Received null message or playerData");
            return;
        }
        
        switch (msg.type) {
            case PLAYER_JOIN:
                if (!msg.playerData.playerId.equals(myPlayerData.playerId)) {
                    OnlinePlayer newPlayer = new OnlinePlayer(msg.playerData);
                    onlinePlayers.put(msg.playerData.playerId, newPlayer);
                    dataManager.updatePlayerPosition(msg.playerData.playerId, msg.playerData.position);
                    dataManager.updatePlayerLocation(msg.playerData.playerId, msg.playerData.currentLocation);
                    System.out.println("Player joined: " + msg.playerData.playerName + " (" + msg.playerData.playerId + ") at " + msg.playerData.position + " Character: " + msg.playerData.characterImage);
                    System.out.println("Total online players: " + onlinePlayers.size());
                }
                break;
                
            case PLAYER_MOVE:
                if (!msg.playerData.playerId.equals(myPlayerData.playerId)) {
                    OnlinePlayer player = onlinePlayers.get(msg.playerData.playerId);
                    if (player != null) {
                        Point currentPos = player.getPosition();
                        Point newPos = msg.playerData.position;
                        if (currentPos == null || !currentPos.equals(newPos)) {
                            player.updatePosition(newPos);
                            dataManager.updatePlayerPosition(msg.playerData.playerId, newPos);
                            System.out.println("Received position update from server: " + msg.playerData.playerId + " to " + newPos);
                        }
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
                        if (dataManager.shouldUpdateStats(msg.playerData.playerId, msg.playerData.money, msg.playerData.health, msg.playerData.energy)) {
                            player.updateStats(msg.playerData.money, msg.playerData.health, msg.playerData.energy);
                            dataManager.updatePlayerStats(msg.playerData.playerId, msg.playerData.money, msg.playerData.health, msg.playerData.energy);
                            System.out.println("Player stats updated: " + msg.playerData.playerId);
                        }
                    }
                }
                break;
                
            case PLAYER_LOCATION_CHANGE:
                if (!msg.playerData.playerId.equals(myPlayerData.playerId)) {
                    OnlinePlayer player = onlinePlayers.get(msg.playerData.playerId);
                    if (player != null) {
                        if (dataManager.shouldUpdateLocation(msg.playerData.playerId, msg.playerData.currentLocation)) {
                            player.updateLocation(msg.playerData.currentLocation);
                            dataManager.updatePlayerLocation(msg.playerData.playerId, msg.playerData.currentLocation);
                            System.out.println("Player location changed: " + msg.playerData.playerId + " to " + msg.playerData.currentLocation);
                        }
                    }
                }
                break;
                
            case PLAYER_LEAVE:
                if (!msg.playerData.playerId.equals(myPlayerData.playerId)) {
                    onlinePlayers.remove(msg.playerData.playerId);
                    dataManager.removePlayer(msg.playerData.playerId);
                    System.out.println("Player left: " + msg.playerData.playerId);
                }
                break;
        }
    }
    
    public void sendPlayerMove(Point newPosition) {
        Point currentPos = myPlayerData.position;
        if (currentPos == null || !currentPos.equals(newPosition)) {
            if (dataManager.shouldUpdatePosition("localPlayer", newPosition)) {
                myPlayerData.updatePosition(newPosition);
                if (isConnected) {
                    sendMessage(NetworkMessage.createPlayerMove(myPlayerData.playerId, newPosition));
                    dataManager.updatePlayerPosition("localPlayer", newPosition);
                    System.out.println("Sent position update: " + newPosition);
                }
            }
        }
    }
    
    public void sendPlayerStatsUpdate(int money, int health, int energy) {
        if (dataManager.shouldUpdateStats("localPlayer", money, health, energy)) {
            myPlayerData.updateStats(money, health, energy);
            if (isConnected) {
                sendMessage(NetworkMessage.createPlayerStatsUpdate(myPlayerData.playerId, money, health, energy));
                dataManager.updatePlayerStats("localPlayer", money, health, energy);
            }
        }
    }
    
    public void sendPlayerLocationChange(PlayerState.Location location) {
        if (dataManager.shouldUpdateLocation("localPlayer", location)) {
            myPlayerData.updateLocation(location);
            if (isConnected) {
                sendMessage(NetworkMessage.createPlayerLocationChange(myPlayerData.playerId, location));
                dataManager.updatePlayerLocation("localPlayer", location);
            }
        }
    }
    
    public void sendPlayerUpdate() {
        if (isConnected) {
            sendMessage(NetworkMessage.createPlayerUpdate(myPlayerData));
        }
    }
    
    public void updateCharacterImage(String characterImagePath) {
        myPlayerData.updateCharacter(characterImagePath);
        if (isConnected) {
            sendMessage(NetworkMessage.createPlayerUpdate(myPlayerData));
        }
    }
    
    private void sendMessage(NetworkMessage msg) {
        try {
            if (out != null && isConnected) {
                out.writeObject(msg);
                out.flush();
            }
        } catch (IOException e) {
            System.out.println("Send error: " + e.getMessage());
            isConnected = false;
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
                "Please check if server is running\n" +
                "Game will close in 10 seconds...", 
                javax.swing.JOptionPane.ERROR_MESSAGE);
            optionPane.setFont(util.FontManager.getFontForText("Server connection error", 14));
            
            javax.swing.JDialog dialog = optionPane.createDialog(null, "Connection Error");
            dialog.setFont(util.FontManager.getFontForText("Connection Error", 14));
            dialog.setVisible(true);
            
            new javax.swing.Timer(10000, e -> {
                System.exit(0);
            }).start();
        });
    }
}
