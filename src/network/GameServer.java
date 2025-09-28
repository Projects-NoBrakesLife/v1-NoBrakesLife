package network;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.*;

public class GameServer extends JFrame {
    private static final int PORT = 12345;
    private ServerSocket serverSocket;
    private Map<String, OnlinePlayer> onlinePlayers = new ConcurrentHashMap<>();
    private Map<Socket, String> clientConnections = new ConcurrentHashMap<>();
    private JTextArea logArea;
    private JLabel playerCountLabel;
    private boolean isRunning = false;
    
    public GameServer() {
        setTitle("Game Server - Debug");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        
        setupUI();
    }
    
    private void setupUI() {
        setLayout(new BorderLayout());
        
        JPanel topPanel = new JPanel(new FlowLayout());
        JButton startBtn = new JButton("Start Server");
        JButton stopBtn = new JButton("Stop Server");
        playerCountLabel = new JLabel("Players: 0");
        
        startBtn.addActionListener(e -> startServer());
        stopBtn.addActionListener(e -> stopServer());
        
        topPanel.add(startBtn);
        topPanel.add(stopBtn);
        topPanel.add(playerCountLabel);
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void startServer() {
        if (isRunning) return;
        
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                isRunning = true;
                log("Server started on port " + PORT);
                
                while (isRunning) {
                    Socket client = serverSocket.accept();
                    log("Client connected: " + client.getInetAddress());
                    handleClient(client);
                }
            } catch (IOException e) {
                if (isRunning) {
                    log("Server error: " + e.getMessage());
                }
            }
        }).start();
    }
    
    private void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            for (Socket client : clientConnections.keySet()) {
                client.close();
            }
            clientConnections.clear();
            onlinePlayers.clear();
            log("Server stopped");
            updatePlayerCount();
        } catch (IOException e) {
            log("Stop error: " + e.getMessage());
        }
    }
    
    private void handleClient(Socket client) {
        new Thread(() -> {
            try {
                ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(client.getInputStream());
                clientOutputs.put(client, out);
                
                for (OnlinePlayer player : onlinePlayers.values()) {
                    NetworkMessage joinMsg = NetworkMessage.createPlayerJoin(player.playerId, player.playerName, player.position, player.characterImage);
                    out.writeObject(joinMsg);
                }
                
                while (isRunning && !client.isClosed()) {
                    NetworkMessage msg = (NetworkMessage) in.readObject();
                    handleMessage(msg, client, out);
                }
            } catch (Exception e) {
                log("Client handler error: " + e.getMessage());
            } finally {
                try {
                    clientOutputs.remove(client);
                    client.close();
                } catch (IOException e) {
                    log("Close client error: " + e.getMessage());
                }
            }
        }).start();
    }
    
    private void handleMessage(NetworkMessage msg, Socket sender, ObjectOutputStream senderOut) {
        switch (msg.type) {
            case PLAYER_JOIN:
                OnlinePlayer newPlayer = new OnlinePlayer(msg.playerId, msg.playerName, msg.position, msg.characterImage);
                onlinePlayers.put(msg.playerId, newPlayer);
                clientConnections.put(sender, msg.playerId);
                log("Player joined: " + msg.playerName + " (" + msg.playerId + ") at " + msg.position + " with " + msg.characterImage);
                broadcastMessage(msg, sender);
                updatePlayerCount();
                break;
                
            case PLAYER_MOVE:
                OnlinePlayer player = onlinePlayers.get(msg.playerId);
                if (player != null) {
                    player.updatePosition(msg.position);
                    log("Player moved: " + msg.playerId + " to " + msg.position);
                    broadcastMessage(msg, sender);
                }
                break;
                
            case PLAYER_LEAVE:
                onlinePlayers.remove(msg.playerId);
                clientConnections.remove(sender);
                log("Player left: " + msg.playerId);
                broadcastMessage(msg, sender);
                updatePlayerCount();
                break;
        }
    }
    
    private Map<Socket, ObjectOutputStream> clientOutputs = new ConcurrentHashMap<>();
    
    private void broadcastMessage(NetworkMessage msg, Socket exclude) {
        for (Map.Entry<Socket, String> entry : clientConnections.entrySet()) {
            Socket client = entry.getKey();
            if (client != exclude && !client.isClosed()) {
                try {
                    ObjectOutputStream out = clientOutputs.get(client);
                    if (out == null) {
                        out = new ObjectOutputStream(client.getOutputStream());
                        clientOutputs.put(client, out);
                    }
                    out.writeObject(msg);
                } catch (IOException e) {
                    log("Broadcast error to " + entry.getValue() + ": " + e.getMessage());
                    clientOutputs.remove(client);
                }
            }
        }
    }
    
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + new Date() + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    private void updatePlayerCount() {
        SwingUtilities.invokeLater(() -> {
            playerCountLabel.setText("Players: " + onlinePlayers.size());
        });
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new GameServer().setVisible(true);
        });
    }
}
