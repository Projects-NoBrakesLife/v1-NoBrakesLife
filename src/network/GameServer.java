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
    private final Map<String, OnlinePlayer> onlinePlayers = new ConcurrentHashMap<>();
    private final Map<Socket, String> clientConnections = new ConcurrentHashMap<>();
    private final Map<Socket, ObjectOutputStream> clientOutputs = new ConcurrentHashMap<>();
    private JTextArea logArea;
    private JLabel playerCountLabel;
    private JButton startBtn;
    private JButton stopBtn;
    private JList<String> playerList;
    private DefaultListModel<String> playerListModel;
    private JButton kickBtn;
    private JLabel waitingLabel;
    private boolean isRunning = false;
    private boolean gameStarted = false;
    private String currentTurnPlayer = null;
    private java.util.List<String> playerTurnOrder = new java.util.ArrayList<>();
    private int nextPlayerNumber = 1;
    private java.util.Map<String, String> playerIdToDisplayId = new java.util.HashMap<>();
    
    public GameServer() {
        setTitle("Game Server - Debug");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        setupUI();
    }
    
    private void setupUI() {
        setLayout(new BorderLayout());
        
 
        JPanel topPanel = new JPanel(new FlowLayout());
        
        startBtn = new JButton("Start Server");
        stopBtn = new JButton("Stop Server");
        playerCountLabel = new JLabel("Players: 0");
        waitingLabel = new JLabel("Waiting for players...");
        
        stopBtn.setEnabled(false);
        
        startBtn.addActionListener(e -> startServer());
        stopBtn.addActionListener(e -> stopServer());
        
        topPanel.add(startBtn);
        topPanel.add(stopBtn);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(playerCountLabel);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(waitingLabel);
        

        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
    
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Server Log"));
        
        JPanel playerPanel = new JPanel(new BorderLayout());
        playerPanel.setBorder(BorderFactory.createTitledBorder("Online Players"));
        
        playerListModel = new DefaultListModel<>();
        playerList = new JList<>(playerListModel);
        playerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playerList.setCellRenderer(new PlayerListCellRenderer());
        JScrollPane playerScroll = new JScrollPane(playerList);
        
        kickBtn = new JButton("Kick Player");
        kickBtn.setEnabled(false);
        kickBtn.addActionListener(e -> kickSelectedPlayer());
        
        playerList.addListSelectionListener(e -> {
            kickBtn.setEnabled(!playerList.isSelectionEmpty());
        });
        
        playerPanel.add(playerScroll, BorderLayout.CENTER);
        playerPanel.add(kickBtn, BorderLayout.SOUTH);
        
        centerSplit.setLeftComponent(logScroll);
        centerSplit.setRightComponent(playerPanel);
        centerSplit.setDividerLocation(500);
        
        add(topPanel, BorderLayout.NORTH);
        add(centerSplit, BorderLayout.CENTER);
    }
    
    private void startServer() {
        if (isRunning) return;
        
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                isRunning = true;
                log("Server started on port " + PORT);
                
                SwingUtilities.invokeLater(() -> {
                    startBtn.setEnabled(false);
                    startBtn.setText("Server Running");
                    stopBtn.setEnabled(true);
                });
                
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
            updatePlayerList();
            
            SwingUtilities.invokeLater(() -> {
                startBtn.setEnabled(true);
                startBtn.setText("Start Server");
                stopBtn.setEnabled(false);
            });
        } catch (IOException e) {
            log("Stop error: " + e.getMessage());
        }
    }
    
    private void handleClient(Socket client) {
        new Thread(() -> {
            ObjectOutputStream out = null;
            ObjectInputStream in = null;
            String clientId = client.getInetAddress().toString() + ":" + client.getPort();
            
            try {
                out = new ObjectOutputStream(client.getOutputStream());
                in = new ObjectInputStream(client.getInputStream());
                clientOutputs.put(client, out);
                
                log("Client connected: " + clientId);
                Thread.sleep(100);
                
                for (OnlinePlayer player : onlinePlayers.values()) {
                    if (player != null && player.getPlayerData() != null) {
                        NetworkMessage joinMsg = NetworkMessage.createPlayerJoin(player.getPlayerData());
                        out.writeObject(joinMsg);
                        out.flush();
                        String displayId = playerIdToDisplayId.get(player.getPlayerId());
                        log("Sent existing player data to " + clientId + ": " + displayId + " (" + player.getPlayerId() + ")");
                    }
                }
                
                while (isRunning && !client.isClosed()) {
                    try {
                        NetworkMessage msg = (NetworkMessage) in.readObject();
                        if (msg != null) {
                            log("Received message from " + clientId + ": " + msg.type);
                            handleMessage(msg, client, out);
                        }
                    } catch (java.io.EOFException e) {
                        log("Client " + clientId + " disconnected (EOF)");
                        break;
                    } catch (java.net.SocketException e) {
                        log("Client " + clientId + " disconnected (Socket)");
                        break;
                    } catch (java.io.IOException e) {
                        log("Client " + clientId + " IO error: " + e.getMessage());
                        break;
                    }
                }
            } catch (Exception e) {
                log("Client handler error for " + clientId + ": " + e.getMessage());
            } finally {
                try {
                    String playerId = clientConnections.get(client);
                        if (playerId != null) {
                            OnlinePlayer player = onlinePlayers.get(playerId);
                            if (player != null) {
                                onlinePlayers.remove(playerId);
                                dataManager.removePlayer(playerId);
                                log("Player disconnected: " + playerId + " (" + clientId + ")");
                                
                                NetworkMessage leaveMsg = new NetworkMessage(NetworkMessage.MessageType.PLAYER_LEAVE, player.getPlayerData());
                                broadcastMessage(leaveMsg, null);
                                updatePlayerCount();
                                updatePlayerList();
                            }
                            clientConnections.remove(client);
                        }
                    clientOutputs.remove(client);
                    if (out != null) {
                        out.close();
                    }
                    if (in != null) {
                        in.close();
                    }
                    client.close();
                    log("Client " + clientId + " cleanup completed");
                } catch (IOException e) {
                    log("Close client error for " + clientId + ": " + e.getMessage());
                }
            }
        }).start();
    }
    
    private OnlineDataManager dataManager = new OnlineDataManagerImpl();
    
    private void initializeTurnSystem() {
        playerTurnOrder.clear();
        playerTurnOrder.addAll(onlinePlayers.keySet());
        java.util.Collections.sort(playerTurnOrder);
        
        if (!playerTurnOrder.isEmpty()) {
            currentTurnPlayer = playerTurnOrder.get(0);
            String displayId = playerIdToDisplayId.get(currentTurnPlayer);
            log("Turn system initialized. First turn: " + displayId + " (" + currentTurnPlayer + ")");
            broadcastTurnChange();
        }
    }
    
    private void nextTurn() {
        if (playerTurnOrder.isEmpty()) return;
        
        int currentIndex = playerTurnOrder.indexOf(currentTurnPlayer);
        int nextIndex = (currentIndex + 1) % playerTurnOrder.size();
        currentTurnPlayer = playerTurnOrder.get(nextIndex);
        
        String displayId = playerIdToDisplayId.get(currentTurnPlayer);
        log("Turn changed to: " + displayId + " (" + currentTurnPlayer + ")");
        broadcastTurnChange();
    }
    
    private void broadcastTurnChange() {
        if (currentTurnPlayer != null) {
            NetworkMessage turnMsg = NetworkMessage.createTurnChange(currentTurnPlayer);
            broadcastMessage(turnMsg, null);
        }
    }
    
    private void handleMessage(NetworkMessage msg, Socket sender, ObjectOutputStream senderOut) {
        if (msg == null || msg.playerData == null) {
            log("Received null message or playerData");
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        switch (msg.type) {
            case PLAYER_JOIN:
                if (msg.playerData.playerId != null && !msg.playerData.playerId.isEmpty()) {
                    if (!onlinePlayers.containsKey(msg.playerData.playerId)) {
                        String displayId = "P" + nextPlayerNumber;
                        playerIdToDisplayId.put(msg.playerData.playerId, displayId);
                        nextPlayerNumber++;
                        
                        OnlinePlayer newPlayer = new OnlinePlayer(msg.playerData);
                        onlinePlayers.put(msg.playerData.playerId, newPlayer);
                        clientConnections.put(sender, msg.playerData.playerId);
                        dataManager.updatePlayerPosition(msg.playerData.playerId, msg.playerData.position);
                        dataManager.updatePlayerLocation(msg.playerData.playerId, msg.playerData.currentLocation);
                        log("Player joined: " + msg.playerData.playerName + " (" + displayId + ") at " + msg.playerData.position + " Character: " + msg.playerData.characterImage);
                        log("Total players now: " + onlinePlayers.size());
                        broadcastMessage(msg, sender);
                        updatePlayerCount();
                        updatePlayerList();
                        
                        if (currentTurnPlayer == null) {
                            initializeTurnSystem();
                        } else {
                            NetworkMessage turnMsg = NetworkMessage.createTurnChange(currentTurnPlayer);
                            try {
                                ObjectOutputStream out = clientOutputs.get(sender);
                                if (out != null) {
                                    out.writeObject(turnMsg);
                                    out.flush();
                                    log("Sent current turn info to new player: " + displayId);
                                }
                            } catch (IOException e) {
                                log("Error sending turn info to new player: " + e.getMessage());
                            }
                        }
                    } else {
                        log("Player " + msg.playerData.playerId + " already exists, updating data");
                        OnlinePlayer existingPlayer = onlinePlayers.get(msg.playerData.playerId);
                        existingPlayer.updateFromPlayerData(msg.playerData);
                        broadcastMessage(msg, sender);
                    }
                }
                break;
                
            case PLAYER_MOVE:
                OnlinePlayer player = onlinePlayers.get(msg.playerData.playerId);
                if (player != null) {
                    Point currentPos = player.getPosition();
                    Point newPos = msg.playerData.position;
                    if (currentPos == null || !currentPos.equals(newPos)) {
                        player.updatePosition(newPos);
                        dataManager.updatePlayerPosition(msg.playerData.playerId, newPos);
                        log("Player moved: " + msg.playerData.playerId + " to " + newPos);
                        broadcastMessage(msg, sender);
                    }
                } else {
                    log("Player not found for move: " + msg.playerData.playerId);
                }
                break;
                
            case PLAYER_UPDATE:
                OnlinePlayer updatePlayer = onlinePlayers.get(msg.playerData.playerId);
                if (updatePlayer != null) {
                    updatePlayer.updateFromPlayerData(msg.playerData);
                    log("Player updated: " + msg.playerData.playerId);
                    broadcastMessage(msg, sender);
                }
                break;
                
            case PLAYER_STATS_UPDATE:
                OnlinePlayer statsPlayer = onlinePlayers.get(msg.playerData.playerId);
                if (statsPlayer != null) {
                    if (dataManager.shouldUpdateStats(msg.playerData.playerId, msg.playerData.money, msg.playerData.health, msg.playerData.energy)) {
                        statsPlayer.updateStats(msg.playerData.money, msg.playerData.health, msg.playerData.energy);
                        dataManager.updatePlayerStats(msg.playerData.playerId, msg.playerData.money, msg.playerData.health, msg.playerData.energy);
                        log("Player stats updated: " + msg.playerData.playerId);
                        broadcastMessage(msg, sender);
                    }
                } else {
                    log("Player not found for stats update: " + msg.playerData.playerId);
                }
                break;
                
            case PLAYER_LOCATION_CHANGE:
                OnlinePlayer locationPlayer = onlinePlayers.get(msg.playerData.playerId);
                if (locationPlayer != null) {
                    if (dataManager.shouldUpdateLocation(msg.playerData.playerId, msg.playerData.currentLocation)) {
                        locationPlayer.updateLocation(msg.playerData.currentLocation);
                        dataManager.updatePlayerLocation(msg.playerData.playerId, msg.playerData.currentLocation);
                        log("Player location changed: " + msg.playerData.playerId + " to " + msg.playerData.currentLocation);
                        broadcastMessage(msg, sender);
                    }
                }
                break;
                
            case PLAYER_LEAVE:
                String displayId = playerIdToDisplayId.get(msg.playerData.playerId);
                onlinePlayers.remove(msg.playerData.playerId);
                clientConnections.remove(sender);
                playerTurnOrder.remove(msg.playerData.playerId);
                playerIdToDisplayId.remove(msg.playerData.playerId);
                if (currentTurnPlayer != null && currentTurnPlayer.equals(msg.playerData.playerId)) {
                    nextTurn();
                }
                log("Player left: " + displayId + " (" + msg.playerData.playerId + ")");
                broadcastMessage(msg, sender);
                updatePlayerCount();
                break;
                
            case TURN_COMPLETE:
                if (currentTurnPlayer != null && currentTurnPlayer.equals(msg.playerData.playerId)) {
                    String playerDisplayId = playerIdToDisplayId.get(msg.playerData.playerId);
                    log("Player " + playerDisplayId + " (" + msg.playerData.playerId + ") completed their turn");
                    nextTurn();
                }
                break;
                
            case TURN_CHANGE:
                log("Turn change message received (server initiated)");
                break;
        }
    }
    
    private void broadcastMessage(NetworkMessage msg, Socket exclude) {
        java.util.List<Socket> toRemove = new java.util.ArrayList<>();
        for (Map.Entry<Socket, String> entry : clientConnections.entrySet()) {
            Socket client = entry.getKey();
            if (client != exclude && !client.isClosed()) {
                try {
                    ObjectOutputStream out = clientOutputs.get(client);
                    if (out != null) {
                        out.writeObject(msg);
                        out.flush();
                    }
                } catch (IOException e) {
                    log("Broadcast error to " + entry.getValue() + ": " + e.getMessage());
                    toRemove.add(client);
                }
            }
        }
        
        for (Socket client : toRemove) {
            clientOutputs.remove(client);
            clientConnections.remove(client);
        }
    }
    
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new Date());
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    private void updatePlayerCount() {
        SwingUtilities.invokeLater(() -> {
            int playerCount = onlinePlayers.size();
            playerCountLabel.setText("Players: " + playerCount);
            
            if (playerCount >= 4) {
                waitingLabel.setText("Ready to start! (4/4)");
                waitingLabel.setForeground(Color.GREEN);
                gameStarted = true;
            } else {
                waitingLabel.setText("Waiting for players... (" + playerCount + "/4)");
                waitingLabel.setForeground(Color.ORANGE);
                gameStarted = false;
            }
        });
    }
    
    private void updatePlayerList() {
        SwingUtilities.invokeLater(() -> {
            playerListModel.clear();
            for (OnlinePlayer player : onlinePlayers.values()) {
                String displayId = playerIdToDisplayId.get(player.getPlayerId());
                String turnIndicator = (currentTurnPlayer != null && currentTurnPlayer.equals(player.getPlayerId())) ? " [TURN]" : "";
                playerListModel.addElement(displayId + ": " + player.getPlayerName() + turnIndicator);
            }
        });
    }
    
    private void kickSelectedPlayer() {
        String selected = playerList.getSelectedValue();
        if (selected == null) return;
        
        String displayId = selected.substring(0, selected.indexOf(":"));
        
        for (Map.Entry<String, String> entry : playerIdToDisplayId.entrySet()) {
            if (entry.getValue().equals(displayId)) {
                String playerId = entry.getKey();
                for (Map.Entry<Socket, String> connEntry : clientConnections.entrySet()) {
                    if (connEntry.getValue().equals(playerId)) {
                        try {
                            connEntry.getKey().close();
                            log("Kicked player: " + displayId + " (" + playerId + ")");
                        } catch (IOException e) {
                            log("Error kicking player: " + e.getMessage());
                        }
                        break;
                    }
                }
                break;
            }
        }
    }
    
    private class PlayerListCellRenderer extends JLabel implements ListCellRenderer<String> {
        private ImageIcon male01Icon;
        private ImageIcon male02Icon;
        private ImageIcon female01Icon;
        private ImageIcon female02Icon;
        
        public PlayerListCellRenderer() {
            setOpaque(true);
            try {
                male01Icon = new ImageIcon("assets/players/Male-01.png");
                male02Icon = new ImageIcon("assets/players/Male-02.png");
                female01Icon = new ImageIcon("assets/players/Female-01.png");
                female02Icon = new ImageIcon("assets/players/Female-02.png");
            } catch (Exception e) {
                System.out.println("Could not load player icons: " + e.getMessage());
            }
        }
        
        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
            setText(value);
            setIcon(null);
            
            if (value != null && value.contains("(") && value.contains(")")) {
                try {
                    String playerId = value.substring(value.indexOf("(") + 1, value.indexOf(")"));
                    OnlinePlayer player = onlinePlayers.get(playerId);
                    if (player != null && player.getCharacterImage() != null) {
                        String characterImage = player.getCharacterImage();
                        if (characterImage.contains("Male-01")) {
                            setIcon(male01Icon);
                        } else if (characterImage.contains("Male-02")) {
                            setIcon(male02Icon);
                        } else if (characterImage.contains("Female-01")) {
                            setIcon(female01Icon);
                        } else if (characterImage.contains("Female-02")) {
                            setIcon(female02Icon);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error rendering player: " + e.getMessage());
                }
            }
            
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            
            return this;
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new GameServer().setVisible(true);
        });
    }
}
