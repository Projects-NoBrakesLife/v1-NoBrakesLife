package network;

import core.Config;
import core.CoreDataManager;
import core.Debug;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.*;

public class GameServer extends JFrame {
    private static final int PORT = 12345;
    private ServerSocket serverSocket;
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
    
    private CoreDataManager coreDataManager;
    
    public GameServer() {
        setTitle("Game Server - Debug");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        coreDataManager = CoreDataManager.getInstance();
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
            coreDataManager.reset();
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
                
                for (PlayerData player : coreDataManager.getAllPlayers().values()) {
                    if (player != null) {
                        NetworkMessage joinMsg = NetworkMessage.createPlayerJoin(player);
                        out.writeObject(joinMsg);
                        out.flush();
                        String displayId = coreDataManager.getPlayerDisplayId(player.playerId);
                        log("Sent existing player data to " + clientId + ": " + displayId + " (" + player.playerId + ")");
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
                        PlayerData player = coreDataManager.getPlayer(playerId);
                        if (player != null) {
                            coreDataManager.removePlayer(playerId);
                            log("Player disconnected: " + playerId + " (" + clientId + ")");
                            
                            NetworkMessage leaveMsg = new NetworkMessage(NetworkMessage.MessageType.PLAYER_LEAVE, player);
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
    
    private void handleMessage(NetworkMessage msg, Socket sender, ObjectOutputStream senderOut) {
        if (msg == null || msg.playerData == null) {
            log("Received null message or playerData");
            return;
        }
        
        switch (msg.type) {
            case PLAYER_JOIN:
                if (msg.playerData.playerId != null && !msg.playerData.playerId.isEmpty()) {
                    PlayerData existingPlayer = coreDataManager.getPlayer(msg.playerData.playerId);
                    if (existingPlayer == null) {
                        PlayerData newPlayer = coreDataManager.addPlayer(
                            msg.playerData.playerId, 
                            msg.playerData.playerName, 
                            msg.playerData.position, 
                            msg.playerData.characterImage
                        );
                        clientConnections.put(sender, msg.playerData.playerId);
                        String displayId = coreDataManager.getPlayerDisplayId(msg.playerData.playerId);
                        Debug.logServer("Player joined: " + msg.playerData.playerName + " (" + displayId + ") at " + msg.playerData.position);
                        Debug.logServer("Total players now: " + coreDataManager.getPlayerCount());
                        broadcastMessage(msg, sender);
                        updatePlayerCount();
                        updatePlayerList();
                        
                        if (coreDataManager.canStartGame() && coreDataManager.getCurrentTurnPlayer() == null) {
                            Debug.logServer("🎮 Starting game with " + coreDataManager.getPlayerCount() + " players");
                            coreDataManager.startGame();
                            
                            for (PlayerData player : coreDataManager.getAllPlayers().values()) {
                                NetworkMessage playerMsg = NetworkMessage.createPlayerJoin(player);
                                broadcastMessage(playerMsg, null);
                                Debug.logServer("📤 Sent player data to all clients: " + player.playerName + " (" + player.playerId + ")");
                            }
                            
                            String currentTurn = coreDataManager.getCurrentTurnPlayer();
                            if (currentTurn != null) {
                                NetworkMessage turnMsg = NetworkMessage.createTurnChange(currentTurn);
                                broadcastMessage(turnMsg, null);
                                Debug.logServer("🎯 Broadcasted game start turn: " + currentTurn);
                            }
                        } else {
                            Debug.logServer("⏳ Waiting for more players to start game: " + coreDataManager.getPlayerCount() + "/" + Config.MIN_PLAYERS_TO_START);
                        }
                    } else {
                        log("Player " + msg.playerData.playerId + " already exists, updating character image");
                        existingPlayer.updateCharacter(msg.playerData.characterImage);
                        log("Updated character image for " + msg.playerData.playerId + " to " + msg.playerData.characterImage);
                        broadcastMessage(msg, sender);
                    }
                }
                break;
                
            case PLAYER_MOVE:
                coreDataManager.updatePlayerPosition(msg.playerData.playerId, msg.playerData.position);
                log("Player moved: " + msg.playerData.playerId + " to " + msg.playerData.position);
                broadcastMessage(msg, sender);
                break;
                
            case PLAYER_UPDATE:
                PlayerData updatePlayer = coreDataManager.getPlayer(msg.playerData.playerId);
                if (updatePlayer != null) {
                    updatePlayer.money = msg.playerData.money;
                    updatePlayer.health = msg.playerData.health;
                    updatePlayer.energy = msg.playerData.energy;
                    updatePlayer.remainingTime = msg.playerData.remainingTime;
                    updatePlayer.position = msg.playerData.position;
                    updatePlayer.currentLocation = msg.playerData.currentLocation;
                    updatePlayer.characterImage = msg.playerData.characterImage;
                    log("Player updated: " + msg.playerData.playerId);
                    broadcastMessage(msg, sender);
                }
                break;
                
            case PLAYER_STATS_UPDATE:
                coreDataManager.updatePlayerStats(msg.playerData.playerId, msg.playerData.money, msg.playerData.health, msg.playerData.energy);
                if (System.currentTimeMillis() % 3000 < 100) {
                    log("Player stats updated: " + msg.playerData.playerId);
                }
                broadcastMessage(msg, sender);
                break;
                
            case PLAYER_LOCATION_CHANGE:
                coreDataManager.updatePlayerLocation(msg.playerData.playerId, msg.playerData.currentLocation);
                log("Player location changed: " + msg.playerData.playerId + " to " + msg.playerData.currentLocation);
                broadcastMessage(msg, sender);
                break;
                
            case PLAYER_TIME_UPDATE:
                coreDataManager.updatePlayerTime(msg.playerData.playerId, msg.playerData.remainingTime);
                if (System.currentTimeMillis() % 5000 < 100) {
                    log("Player time updated: " + msg.playerData.playerId + " to " + msg.playerData.remainingTime + " hours");
                }
                broadcastMessage(msg, sender);
                break;
                
            case PLAYER_LEAVE:
                String displayId = coreDataManager.getPlayerDisplayId(msg.playerData.playerId);
                coreDataManager.removePlayer(msg.playerData.playerId);
                clientConnections.remove(sender);
                log("Player left: " + displayId + " (" + msg.playerData.playerId + ")");
                broadcastMessage(msg, sender);
                updatePlayerCount();
                break;
                
            case TURN_COMPLETE:
                Debug.logServer("Received turn complete from: " + msg.playerData.playerId);
                coreDataManager.completeTurn(msg.playerData.playerId);
      
                String newTurnPlayer = coreDataManager.getCurrentTurnPlayer();
                if (newTurnPlayer != null) {
                    NetworkMessage turnChangeMsg = NetworkMessage.createTurnChange(newTurnPlayer);
                    broadcastMessage(turnChangeMsg, null);
                    Debug.logServer("Broadcasted turn change to: " + newTurnPlayer);
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
            int playerCount = coreDataManager.getPlayerCount();
            playerCountLabel.setText("Players: " + playerCount);
            
            if (playerCount >= core.Config.MIN_PLAYERS_TO_START) {
                waitingLabel.setText("Ready to start! (" + playerCount + "/" + core.Config.MIN_PLAYERS_TO_START + ")");
                waitingLabel.setForeground(Color.GREEN);
                gameStarted = true;
            } else {
                waitingLabel.setText("Waiting for players... (" + playerCount + "/" + core.Config.MIN_PLAYERS_TO_START + ")");
                waitingLabel.setForeground(Color.ORANGE);
                gameStarted = false;
            }
        });
    }
    
    private void updatePlayerList() {
        SwingUtilities.invokeLater(() -> {
            playerListModel.clear();
            String currentTurn = coreDataManager.getCurrentTurnPlayer();
            for (PlayerData player : coreDataManager.getAllPlayers().values()) {
                String displayId = coreDataManager.getPlayerDisplayId(player.playerId);
                String turnIndicator = (currentTurn != null && currentTurn.equals(player.playerId)) ? " [TURN]" : "";
                playerListModel.addElement(displayId + ": " + player.playerName + turnIndicator);
            }
        });
    }
    
    private void kickSelectedPlayer() {
        String selected = playerList.getSelectedValue();
        if (selected == null) return;
        
        String displayId = selected.substring(0, selected.indexOf(":"));
        
        for (PlayerData player : coreDataManager.getAllPlayers().values()) {
            String playerDisplayId = coreDataManager.getPlayerDisplayId(player.playerId);
            if (playerDisplayId.equals(displayId)) {
                for (Map.Entry<Socket, String> connEntry : clientConnections.entrySet()) {
                    if (connEntry.getValue().equals(player.playerId)) {
                        try {
                            connEntry.getKey().close();
                            log("Kicked player: " + displayId + " (" + player.playerId + ")");
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
                    String displayId = value.substring(0, value.indexOf(":"));
                    for (PlayerData player : coreDataManager.getAllPlayers().values()) {
                        String playerDisplayId = coreDataManager.getPlayerDisplayId(player.playerId);
                        if (playerDisplayId.equals(displayId)) {
                            String characterImage = player.characterImage;
                            if (characterImage.contains("Male-01")) {
                                setIcon(male01Icon);
                            } else if (characterImage.contains("Male-02")) {
                                setIcon(male02Icon);
                            } else if (characterImage.contains("Female-01")) {
                                setIcon(female01Icon);
                            } else if (characterImage.contains("Female-02")) {
                                setIcon(female02Icon);
                            }
                            break;
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
