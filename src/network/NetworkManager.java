package network;

import java.awt.Point;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkManager {
    private static final int PORT = 12345;
    private static final String SERVER_IP = "localhost";
    
    private boolean isServer = false;
    private boolean isConnected = false;
    private String myPlayerId;
    private String myPlayerName;
    private Point myPosition;
    private String myCharacterImage;
    
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    
    private Map<String, OnlinePlayer> onlinePlayers = new ConcurrentHashMap<>();
    private List<NetworkMessage> messageQueue = new ArrayList<>();
    
    public NetworkManager(String playerId, String playerName, Point startPosition, String characterImage) {
        this.myPlayerId = playerId;
        this.myPlayerName = playerName;
        this.myPosition = startPosition;
        this.myCharacterImage = characterImage;
    }
    
    public void startServer() {
        isServer = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                System.out.println("Server started on port " + PORT);
                
                while (true) {
                    Socket client = serverSocket.accept();
                    System.out.println("Client connected: " + client.getInetAddress());
                    handleClient(client);
                }
            } catch (IOException e) {
                System.out.println("Server error: " + e.getMessage());
            }
        }).start();
    }
    
    public void connectToServer() {
        new Thread(() -> {
            try {
                clientSocket = new Socket(SERVER_IP, PORT);
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                in = new ObjectInputStream(clientSocket.getInputStream());
                
                isConnected = true;
                System.out.println("Connected to server");
                
                sendMessage(NetworkMessage.createPlayerJoin(myPlayerId, myPlayerName, myPosition, myCharacterImage));
                
                while (isConnected) {
                    NetworkMessage msg = (NetworkMessage) in.readObject();
                    handleMessage(msg);
                }
            } catch (Exception e) {
                System.out.println("Connection error: " + e.getMessage());
            }
        }).start();
    }
    
    private void handleClient(Socket client) {
        new Thread(() -> {
            try {
                ObjectOutputStream clientOut = new ObjectOutputStream(client.getOutputStream());
                ObjectInputStream clientIn = new ObjectInputStream(client.getInputStream());
                
                for (OnlinePlayer player : onlinePlayers.values()) {
                    NetworkMessage joinMsg = NetworkMessage.createPlayerJoin(player.playerId, player.playerName, player.position, player.characterImage);
                    clientOut.writeObject(joinMsg);
                }
                
                NetworkMessage myJoinMsg = NetworkMessage.createPlayerJoin(myPlayerId, myPlayerName, myPosition, myCharacterImage);
                clientOut.writeObject(myJoinMsg);
                
                while (true) {
                    NetworkMessage msg = (NetworkMessage) clientIn.readObject();
                    broadcastMessage(msg, clientOut);
                    handleMessage(msg);
                }
            } catch (Exception e) {
                System.out.println("Client handler error: " + e.getMessage());
            }
        }).start();
    }
    
    private void broadcastMessage(NetworkMessage msg, ObjectOutputStream excludeOut) {
        for (OnlinePlayer player : onlinePlayers.values()) {
            if (!player.playerId.equals(msg.playerId)) {
                try {
                    if (excludeOut != null) {
                        excludeOut.writeObject(msg);
                    }
                } catch (IOException e) {
                    System.out.println("Broadcast error: " + e.getMessage());
                }
            }
        }
    }
    
    private void handleMessage(NetworkMessage msg) {
        switch (msg.type) {
            case PLAYER_JOIN:
                OnlinePlayer newPlayer = new OnlinePlayer(msg.playerId, msg.playerName, msg.position, msg.characterImage);
                onlinePlayers.put(msg.playerId, newPlayer);
                System.out.println("Player joined: " + msg.playerName + " at " + msg.position + " with " + msg.characterImage);
                break;
                
            case PLAYER_MOVE:
                OnlinePlayer player = onlinePlayers.get(msg.playerId);
                if (player != null) {
                    player.updatePosition(msg.position);
                    System.out.println("Player moved: " + msg.playerId + " to " + msg.position);
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
            if (clientSocket != null) clientSocket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            System.out.println("Disconnect error: " + e.getMessage());
        }
    }
}
