package core;

import java.awt.Point;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import network.PlayerData;

public class CoreDataManager {
    public enum GamePhase {
        WAITING_FOR_PLAYERS,
        GAME_STARTING,
        GAME_RUNNING,
        GAME_PAUSED,
        GAME_ENDED
    }

    private static CoreDataManager instance;

    private final Map<String, PlayerData> allPlayers = new ConcurrentHashMap<>();
    private final List<String> playerTurnOrder = new CopyOnWriteArrayList<>();
    private String currentTurnPlayer = null;
    private int nextPlayerNumber = 1;
    private final Map<String, String> playerIdToDisplayId = new ConcurrentHashMap<>();
    private GamePhase currentPhase = GamePhase.WAITING_FOR_PLAYERS;

    private final Object dataLock = new Object();
    private long lastBroadcastTime = 0;
    
    public static CoreDataManager getInstance() {
        if (instance == null) {
            instance = new CoreDataManager();
        }
        return instance;
    }
    
    private CoreDataManager() {
    }
    
    public synchronized PlayerData addPlayer(String playerId, String playerName, Point position, String characterImage) {
        synchronized (dataLock) {
            if (allPlayers.containsKey(playerId)) {
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

            updateGamePhase();

            if (canStartGame() && currentTurnPlayer == null) {
                startGame();
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
                if (currentTurnPlayer != null && currentTurnPlayer.equals(playerId)) {
                    nextTurn();
                }

                if (playerTurnOrder.isEmpty()) {
                    currentTurnPlayer = null;
                } else if (currentTurnPlayer == null) {
                    currentTurnPlayer = playerTurnOrder.get(0);
                }

                updateGamePhase();
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
            if (playerTurnOrder.size() >= GameConfig.Game.MIN_PLAYERS_TO_START) {
                currentTurnPlayer = playerTurnOrder.get(0);
                currentPhase = GamePhase.GAME_RUNNING;
            }
        }
    }

    private synchronized void updateGamePhase() {
        synchronized (dataLock) {
            int playerCount = playerTurnOrder.size();

            if (playerCount < GameConfig.Game.MIN_PLAYERS_TO_START) {
                if (currentPhase != GamePhase.WAITING_FOR_PLAYERS) {
                    currentPhase = GamePhase.WAITING_FOR_PLAYERS;
                }
            } else if (currentPhase == GamePhase.WAITING_FOR_PLAYERS) {
                currentPhase = GamePhase.GAME_STARTING;

                if (currentTurnPlayer == null) {
                    nextTurn();
                }

                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        synchronized (dataLock) {
                            if (currentPhase == GamePhase.GAME_STARTING) {
                                currentPhase = GamePhase.GAME_RUNNING;
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        }
    }
    
    public synchronized boolean canStartGame() {
        synchronized (dataLock) {
            return playerTurnOrder.size() >= GameConfig.Game.MIN_PLAYERS_TO_START;
        }
    }
    
    public synchronized void forceSetTurn(String playerId) {
        synchronized (dataLock) {
            if (playerTurnOrder.contains(playerId)) {
                currentTurnPlayer = playerId;
            }
        }
    }
    
    public synchronized void nextTurn() {
        synchronized (dataLock) {
            if (playerTurnOrder.isEmpty()) {
                currentTurnPlayer = null;
                return;
            }

            int currentIndex = playerTurnOrder.indexOf(currentTurnPlayer);
            if (currentIndex == -1) {
                currentTurnPlayer = playerTurnOrder.get(0);
                return;
            }

            int nextIndex = (currentIndex + 1) % playerTurnOrder.size();
            currentTurnPlayer = playerTurnOrder.get(nextIndex);
        }
    }
    
    public synchronized void completeTurn(String playerId) {
        synchronized (dataLock) {
            if (currentTurnPlayer != null && currentTurnPlayer.equals(playerId)) {
                nextTurn();
            }
        }
    }

    public synchronized void cleanupStaleConnections() {
        synchronized (dataLock) {
            long now = System.currentTimeMillis();
            List<String> toRemove = new ArrayList<>();

            for (Map.Entry<String, PlayerData> entry : allPlayers.entrySet()) {
                if (now - entry.getValue().timestamp > GameConfig.Network.CONNECTION_TIMEOUT) {
                    toRemove.add(entry.getKey());
                }
            }

            for (String playerId : toRemove) {
                removePlayer(playerId);
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
    
    public synchronized List<String> getPlayerTurnOrder() {
        synchronized (dataLock) {
            return new ArrayList<>(playerTurnOrder);
        }
    }

    public synchronized boolean shouldBroadcast() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBroadcastTime >= GameConfig.Network.BROADCAST_INTERVAL) {
            lastBroadcastTime = currentTime;
            return true;
        }
        return false;
    }

    public synchronized GamePhase getCurrentPhase() {
        synchronized (dataLock) {
            return currentPhase;
        }
    }

    public synchronized boolean isWaitingForPlayers() {
        synchronized (dataLock) {
            return currentPhase == GamePhase.WAITING_FOR_PLAYERS;
        }
    }

    public synchronized boolean isGameReady() {
        synchronized (dataLock) {
            return currentPhase == GamePhase.GAME_RUNNING &&
                   playerTurnOrder.size() >= GameConfig.Game.MIN_PLAYERS_TO_START;
        }
    }

    public synchronized void reset() {
        synchronized (dataLock) {
            allPlayers.clear();
            playerTurnOrder.clear();
            playerIdToDisplayId.clear();
            currentTurnPlayer = null;
            nextPlayerNumber = 1;
            currentPhase = GamePhase.WAITING_FOR_PLAYERS;
        }
    }
}
