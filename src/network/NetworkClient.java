package network;

import core.CoreDataManager;
import core.GameConfig;
import core.PlayerState;
import java.awt.Point;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.Timer;

public class NetworkClient {
    private static final String SERVER_IP = "localhost";

    private PlayerData myPlayerData;
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private Map<String, PlayerData> onlinePlayers = new ConcurrentHashMap<>();
    private boolean isConnected = false;
    private java.util.function.Consumer<String> turnChangeCallback;
    private String currentTurnPlayer;

    private CoreDataManager coreDataManager;
    private Timer heartbeatTimer;
    
    public NetworkClient(String playerId, String playerName, Point startPosition, String characterImage) {
        this.myPlayerData = new PlayerData(playerId, playerName, startPosition, characterImage);
        this.coreDataManager = CoreDataManager.getInstance();
        System.out.println("NetworkClient created for: " + playerName + " with character: " + characterImage);
    }
    
    public NetworkClient(PlayerData playerData) {
        this.myPlayerData = playerData.copy();
        this.coreDataManager = CoreDataManager.getInstance();
    }
    
    public void connect() {
        new Thread(() -> {
            int retryCount = 0;
            int maxRetries = GameConfig.Network.RETRY_ATTEMPTS;
            
            while (retryCount < maxRetries && !isConnected) {
                try {
                    clientSocket = new Socket(SERVER_IP, GameConfig.Network.SERVER_PORT);
                    clientSocket.setSoTimeout(5000);
                    out = new ObjectOutputStream(clientSocket.getOutputStream());
                    in = new ObjectInputStream(clientSocket.getInputStream());
                    
                    isConnected = true;
                    System.out.println("Connected to server");

                    Thread.sleep(100);
                    sendMessage(NetworkMessage.createPlayerJoin(myPlayerData));

                    startHeartbeat();
                    
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
                            Thread.sleep(GameConfig.Network.RETRY_DELAY);
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
    
    private void handleMessage(NetworkMessage msg) {
        if (msg == null || msg.playerData == null) {
            System.out.println("Received null message or playerData");
            return;
        }

        switch (msg.type) {
            case PLAYER_JOIN:
                if (!msg.playerData.playerId.equals(myPlayerData.playerId)) {
                    synchronized (onlinePlayers) {
                        PlayerData newPlayer = msg.playerData.copy();
                        onlinePlayers.put(msg.playerData.playerId, newPlayer);
                        System.out.println("Player joined: " + msg.playerData.playerName + " (" + msg.playerData.playerId + ") at " + msg.playerData.position);
                        System.out.println("Total online players: " + onlinePlayers.size());
                    }
                }
                break;

            case PLAYER_MOVE:
                if (!msg.playerData.playerId.equals(myPlayerData.playerId)) {
                    synchronized (onlinePlayers) {
                        PlayerData player = onlinePlayers.get(msg.playerData.playerId);
                        if (player != null) {
                            Point currentPos = player.position;
                            Point newPos = msg.playerData.position;
                            if (currentPos == null || !currentPos.equals(newPos)) {
                                player.updatePosition(newPos);
                            }
                        }
                    }
                }
                break;

            case PLAYER_UPDATE:
                if (!msg.playerData.playerId.equals(myPlayerData.playerId)) {
                    synchronized (onlinePlayers) {
                        PlayerData player = onlinePlayers.get(msg.playerData.playerId);
                        if (player != null) {
                            player.money = msg.playerData.money;
                            player.health = msg.playerData.health;
                            player.energy = msg.playerData.energy;
                            player.remainingTime = msg.playerData.remainingTime;
                            player.position = msg.playerData.position;
                            player.currentLocation = msg.playerData.currentLocation;
                            player.characterImage = msg.playerData.characterImage;
                        }
                    }
                }
                break;

            case PLAYER_STATS_UPDATE:
                if (!msg.playerData.playerId.equals(myPlayerData.playerId)) {
                    synchronized (onlinePlayers) {
                        PlayerData player = onlinePlayers.get(msg.playerData.playerId);
                        if (player != null) {
                            player.updateStats(msg.playerData.money, msg.playerData.health, msg.playerData.energy);
                        }
                    }
                }
                break;

            case PLAYER_LOCATION_CHANGE:
                if (!msg.playerData.playerId.equals(myPlayerData.playerId)) {
                    synchronized (onlinePlayers) {
                        PlayerData player = onlinePlayers.get(msg.playerData.playerId);
                        if (player != null) {
                            player.updateLocation(msg.playerData.currentLocation);
                        }
                    }
                }
                break;

            case PLAYER_TIME_UPDATE:
                if (!msg.playerData.playerId.equals(myPlayerData.playerId)) {
                    synchronized (onlinePlayers) {
                        PlayerData player = onlinePlayers.get(msg.playerData.playerId);
                        if (player != null) {
                            player.updateTime(msg.playerData.remainingTime);
                        }
                    }
                }
                break;

            case PLAYER_LEAVE:
                if (!msg.playerData.playerId.equals(myPlayerData.playerId)) {
                    synchronized (onlinePlayers) {
                        onlinePlayers.remove(msg.playerData.playerId);
                        System.out.println("Player left: " + msg.playerData.playerId);
                    }
                }
                break;

            case TURN_COMPLETE:
                System.out.println("Turn complete message received for: " + msg.playerData.playerId);
                break;

            case GAME_STATE_UPDATE:
                System.out.println("Received game state update: " + msg.playerData.playerCount + " players, game started: " + msg.playerData.gameStarted);
                break;

            case TURN_CHANGE:
                System.out.println("Turn changed to: " + msg.playerData.playerId);
                currentTurnPlayer = msg.playerData.playerId;
                onTurnChanged(msg.playerData.playerId);
                break;

            case HEARTBEAT:
                break;
        }
    }
    
    public void sendPlayerMove(Point newPosition) {
        Point currentPos = myPlayerData.position;
        if (currentPos == null || !currentPos.equals(newPosition)) {
            myPlayerData.updatePosition(newPosition);
            if (isConnected) {
                sendMessage(NetworkMessage.createPlayerMove(myPlayerData.playerId, newPosition));
            }
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
    
    public void sendPlayerTimeUpdate(int remainingTime) {
        myPlayerData.updateTime(remainingTime);
        if (isConnected) {
            sendMessage(NetworkMessage.createPlayerTimeUpdate(myPlayerData.playerId, remainingTime));
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
    
    public void sendTurnComplete() {
        if (isConnected) {
            sendMessage(NetworkMessage.createTurnComplete(myPlayerData.playerId));
            System.out.println("Sent turn complete for: " + myPlayerData.playerId);
        }
    }
    
    public void setTurnChangeCallback(java.util.function.Consumer<String> callback) {
        this.turnChangeCallback = callback;
    }
    
    private void onTurnChanged(String newTurnPlayerId) {
        if (turnChangeCallback != null) {
            turnChangeCallback.accept(newTurnPlayerId);
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
    
    public Map<String, PlayerData> getOnlinePlayers() {
        return onlinePlayers;
    }
    
    public PlayerData getMyPlayerData() {
        return myPlayerData.copy();
    }
    
    public String getCurrentTurnPlayer() {
        return currentTurnPlayer;
    }
    
    public boolean isConnected() {
        return isConnected;
    }
    
    private void startHeartbeat() {
        if (heartbeatTimer != null) {
            heartbeatTimer.stop();
        }

        heartbeatTimer = new Timer(GameConfig.Network.HEARTBEAT_INTERVAL, e -> {
            if (isConnected) {
                sendMessage(NetworkMessage.createHeartbeat(myPlayerData.playerId));
            }
        });
        heartbeatTimer.start();
    }

    private void stopHeartbeat() {
        if (heartbeatTimer != null) {
            heartbeatTimer.stop();
            heartbeatTimer = null;
        }
    }

    public void disconnect() {
        isConnected = false;
        stopHeartbeat();

        try {
            if (out != null) {
                sendMessage(NetworkMessage.createPlayerLeave(myPlayerData.playerId));
            }
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
            
            javax.swing.Timer timer = new javax.swing.Timer(10000, e -> {
                System.exit(0);
            });
            timer.setRepeats(false);
            timer.start();
        });
    }
}
