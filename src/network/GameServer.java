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
    private JButton startBtn;
    private JButton stopBtn;
    private JList<String> playerList;
    private DefaultListModel<String> playerListModel;
    private JButton kickBtn;
    private boolean isRunning = false;
    
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
        
        stopBtn.setEnabled(false);
        
        startBtn.addActionListener(e -> startServer());
        stopBtn.addActionListener(e -> stopServer());
        
        topPanel.add(startBtn);
        topPanel.add(stopBtn);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(playerCountLabel);
        

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
                    String playerId = clientConnections.get(client);
                    if (playerId != null) {
                        onlinePlayers.remove(playerId);
                        clientConnections.remove(client);
                        log("Player disconnected: " + playerId);
                        
                        NetworkMessage leaveMsg = new NetworkMessage(NetworkMessage.MessageType.PLAYER_LEAVE, playerId);
                        broadcastMessage(leaveMsg, null);
                        updatePlayerCount();
                        updatePlayerList();
                    }
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
                updatePlayerList();
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
            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new Date());
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    private void updatePlayerCount() {
        SwingUtilities.invokeLater(() -> {
            playerCountLabel.setText("Players: " + onlinePlayers.size());
        });
    }
    
    private void updatePlayerList() {
        SwingUtilities.invokeLater(() -> {
            playerListModel.clear();
            for (OnlinePlayer player : onlinePlayers.values()) {
                playerListModel.addElement(player.playerName + " (" + player.playerId + ")");
            }
        });
    }
    
    private void kickSelectedPlayer() {
        String selected = playerList.getSelectedValue();
        if (selected == null) return;
        
    
        String playerId = selected.substring(selected.indexOf("(") + 1, selected.indexOf(")"));
        
     
        for (Map.Entry<Socket, String> entry : clientConnections.entrySet()) {
            if (entry.getValue().equals(playerId)) {
                try {
                    entry.getKey().close();
                    log("Kicked player: " + playerId);
                } catch (IOException e) {
                    log("Error kicking player: " + e.getMessage());
                }
                break;
            }
        }
    }
    
    private class PlayerListCellRenderer extends JLabel implements ListCellRenderer<String> {
        private ImageIcon male01Icon;
        private ImageIcon male02Icon;
        
        public PlayerListCellRenderer() {
            setOpaque(true);
            try {
                male01Icon = new ImageIcon("assets/players/Male-01.png");
                male02Icon = new ImageIcon("assets/players/Male-02.png");
            } catch (Exception e) {
                System.out.println("Could not load player icons: " + e.getMessage());
            }
        }
        
        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
            setText(value);
            
            if (value != null) {
                String playerId = value.substring(value.indexOf("(") + 1, value.indexOf(")"));
                OnlinePlayer player = onlinePlayers.get(playerId);
                if (player != null) {
                    if (player.characterImage.contains("Male-01")) {
                        setIcon(male01Icon);
                    } else if (player.characterImage.contains("Male-02")) {
                        setIcon(male02Icon);
                    }
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
