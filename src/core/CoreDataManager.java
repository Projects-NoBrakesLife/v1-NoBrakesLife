package core;

import java.awt.Point;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import network.PlayerData;

public class CoreDataManager {
    private static CoreDataManager instance;
    
    private final Map<String, PlayerData> allPlayers = new ConcurrentHashMap<>();
    private final List<String> playerTurnOrder = new CopyOnWriteArrayList<>();
    private String currentTurnPlayer = null;
    private int nextPlayerNumber = 1;
    private final Map<String, String> playerIdToDisplayId = new ConcurrentHashMap<>();
    
    private final Object dataLock = new Object();
    private long lastBroadcastTime = 0;
    private static final long BROADCAST_INTERVAL = 100;
    
    public static CoreDataManager getInstance() {
        if (instance == null) {
            instance = new CoreDataManager();
        }
        return instance;
    }
    
    private CoreDataManager() {
        Debug.log("CoreDataManager initialized");
    }
    
    public synchronized PlayerData addPlayer(String playerId, String playerName, Point position, String characterImage) {
        synchronized (dataLock) {
            if (allPlayers.containsKey(playerId)) {
                Debug.logPlayer(playerId, "Player already exists, updating data");
                PlayerData existing = allPlayers.get(playerId);
                existing.updateCharacter(characterImage);
                return existing;
            }
            
            String displayId = "P" + nextPlayerNumber;
            playerIdToDisplayId.put(playerId, displayId);
            nextPlayerNumber++;
            
            PlayerData newPlayer = new PlayerData(playerId, playerName, position, characterImage);
            allPlayers.put(playerId, newPlayer);
            playerTurnOrder.add(playerId);
            Collections.sort(playerTurnOrder);
            
            Debug.logPlayer(playerId, "Added player: " + playerName + " (" + displayId + ") at " + position);
            
            if (canStartGame() && currentTurnPlayer == null) {
                startGame();
                Debug.logTurn("🎮 Game auto-started with " + playerTurnOrder.size() + " players! First turn: " + displayId);
                
                Debug.logTurn("📢 Broadcasting game start signal to all clients");
            } else {
                Debug.logTurn("Player added, total players: " + playerTurnOrder.size() + ", waiting for game to start");
            }
            
            return newPlayer;
        }
    }
    
    public synchronized void removePlayer(String playerId) {
        synchronized (dataLock) {
            PlayerData removed = allPlayers.remove(playerId);
            playerTurnOrder.remove(playerId);
            playerIdToDisplayId.remove(playerId);
            
            if (removed != null) {
                Debug.logPlayer(playerId, "Removed player: " + playerId);
                
                if (currentTurnPlayer != null && currentTurnPlayer.equals(playerId)) {
                    nextTurn();
                }
                
             
                if (playerTurnOrder.isEmpty()) {
                    currentTurnPlayer = null;
                    Debug.logTurn("No players left, resetting turn to null");
                } else if (currentTurnPlayer == null) {
                    currentTurnPlayer = playerTurnOrder.get(0);
                    String currentDisplayId = playerIdToDisplayId.get(currentTurnPlayer);
                    Debug.logTurn("Set turn to first remaining player: " + currentDisplayId + " (" + currentTurnPlayer + ")");
                }
            }
        }
    }
    
    public synchronized void updatePlayerPosition(String playerId, Point position) {
        synchronized (dataLock) {
            PlayerData player = allPlayers.get(playerId);
            if (player != null) {
                player.updatePosition(position);
            }
        }
    }
    
    public synchronized void updatePlayerStats(String playerId, int money, int health, int energy) {
        synchronized (dataLock) {
            PlayerData player = allPlayers.get(playerId);
            if (player != null) {
                player.updateStats(money, health, energy);
            }
        }
    }
    
    public synchronized void updatePlayerTime(String playerId, int remainingTime) {
        synchronized (dataLock) {
            PlayerData player = allPlayers.get(playerId);
            if (player != null) {
                player.updateTime(remainingTime);
            }
        }
    }
    
    public synchronized void updatePlayerLocation(String playerId, PlayerState.Location location) {
        synchronized (dataLock) {
            PlayerData player = allPlayers.get(playerId);
            if (player != null) {
                player.updateLocation(location);
            }
        }
    }
    
    public synchronized void startGame() {
        synchronized (dataLock) {
            if (playerTurnOrder.size() >= Config.MIN_PLAYERS_TO_START) {
                currentTurnPlayer = playerTurnOrder.get(0);
                String currentDisplayId = playerIdToDisplayId.get(currentTurnPlayer);
                Debug.logTurn("🎮 Game started! First turn player: " + currentDisplayId + " (" + currentTurnPlayer + ")");
            } else {
                Debug.logTurn("❌ Cannot start game, not enough players: " + playerTurnOrder.size() + "/" + Config.MIN_PLAYERS_TO_START);
            }
        }
    }
    
    public synchronized boolean canStartGame() {
        synchronized (dataLock) {
            return playerTurnOrder.size() >= Config.MIN_PLAYERS_TO_START;
        }
    }
    
    public synchronized void forceSetTurn(String playerId) {
        synchronized (dataLock) {
            if (playerTurnOrder.contains(playerId)) {
                currentTurnPlayer = playerId;
                String currentDisplayId = playerIdToDisplayId.get(currentTurnPlayer);
                Debug.logTurn("Force set turn to: " + currentDisplayId + " (" + currentTurnPlayer + ")");
            } else {
                Debug.logTurn("Player " + playerId + " not found in turn order, cannot force set turn");
            }
        }
    }
    
    public synchronized void nextTurn() {
        synchronized (dataLock) {
            if (playerTurnOrder.isEmpty()) {
                currentTurnPlayer = null;
                Debug.logTurn("No players in turn order, setting current turn to null");
                return;
            }

            int currentIndex = playerTurnOrder.indexOf(currentTurnPlayer);
            if (currentIndex == -1) {
               
                currentTurnPlayer = playerTurnOrder.get(0);
                String currentDisplayId = playerIdToDisplayId.get(currentTurnPlayer);
                Debug.logTurn("Current player not found in order, setting to first player: " + currentDisplayId);
                return;
            }

            int nextIndex = (currentIndex + 1) % playerTurnOrder.size();
            String previousPlayer = currentTurnPlayer;
            currentTurnPlayer = playerTurnOrder.get(nextIndex);

            String previousDisplayId = playerIdToDisplayId.get(previousPlayer);
            String currentDisplayId = playerIdToDisplayId.get(currentTurnPlayer);
            Debug.logTurn("Turn changed from " + previousDisplayId + " to " + currentDisplayId + " (index: " + currentIndex + " -> " + nextIndex + ")");
        }
    }
    
    public synchronized void completeTurn(String playerId) {
        synchronized (dataLock) {
            if (currentTurnPlayer != null && currentTurnPlayer.equals(playerId)) {
                String playerDisplayId = playerIdToDisplayId.get(playerId);
                Debug.logTurn("Player " + playerDisplayId + " completed their turn");
                nextTurn();
                
      
                if (currentTurnPlayer != null) {
                    String newTurnDisplayId = playerIdToDisplayId.get(currentTurnPlayer);
                    Debug.logTurn("Broadcasting turn change to: " + newTurnDisplayId + " (" + currentTurnPlayer + ")");
                }
            }
        }
    }
    
    public synchronized PlayerData getPlayer(String playerId) {
        synchronized (dataLock) {
            return allPlayers.get(playerId);
        }
    }
    
    public synchronized Map<String, PlayerData> getAllPlayers() {
        synchronized (dataLock) {
            return new ConcurrentHashMap<>(allPlayers);
        }
    }
    
    public synchronized String getCurrentTurnPlayer() {
        synchronized (dataLock) {
            return currentTurnPlayer;
        }
    }
    
    public synchronized boolean isPlayerTurn(String playerId) {
        synchronized (dataLock) {
            return currentTurnPlayer != null && currentTurnPlayer.equals(playerId);
        }
    }
    
    public synchronized String getPlayerDisplayId(String playerId) {
        synchronized (dataLock) {
            return playerIdToDisplayId.get(playerId);
        }
    }
    
    public synchronized int getPlayerCount() {
        synchronized (dataLock) {
            return allPlayers.size();
        }
    }
    
    public synchronized boolean isGameReady() {
        synchronized (dataLock) {
            return allPlayers.size() >= Config.MIN_PLAYERS_TO_START;
        }
    }
    
    public synchronized List<String> getPlayerTurnOrder() {
        synchronized (dataLock) {
            return new ArrayList<>(playerTurnOrder);
        }
    }
    
    public synchronized boolean shouldBroadcast() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBroadcastTime >= BROADCAST_INTERVAL) {
            lastBroadcastTime = currentTime;
            return true;
        }
        return false;
    }
    
    public synchronized void reset() {
        synchronized (dataLock) {
            allPlayers.clear();
            playerTurnOrder.clear();
            playerIdToDisplayId.clear();
            currentTurnPlayer = null;
            nextPlayerNumber = 1;
            Debug.log("CoreDataManager reset");
        }
    }
}
