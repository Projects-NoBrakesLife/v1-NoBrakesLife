package core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameStateManager {
    public enum GamePhase {
        WAITING_FOR_PLAYERS,
        GAME_STARTING,
        GAME_RUNNING,
        GAME_PAUSED,
        GAME_ENDED
    }
    
    private GamePhase currentPhase = GamePhase.WAITING_FOR_PLAYERS;
    private final Map<String, PlayerGameState> playerStates = new ConcurrentHashMap<>();
    private final Set<String> connectedPlayers = new HashSet<>();
    private String currentTurnPlayer = null;
    private long lastStateUpdate = 0;
    private final Object stateLock = new Object();
    
    private static GameStateManager instance;
    
    public static GameStateManager getInstance() {
        if (instance == null) {
            instance = new GameStateManager();
        }
        return instance;
    }
    
    private GameStateManager() {
        Debug.log("GameStateManager initialized");
    }
    
    public synchronized boolean addPlayer(String playerId, String playerName, String characterImage) {
        synchronized (stateLock) {
            if (playerStates.containsKey(playerId)) {
                Debug.log("Player " + playerId + " already exists, updating character image");
                PlayerGameState existingState = playerStates.get(playerId);
                if (existingState != null && !existingState.getCharacterImage().equals(characterImage)) {
                    existingState.updateCharacterImage(characterImage);
                    Debug.log("Updated character image for " + playerId + " to " + characterImage);
                }
                return false;
            }
            
            PlayerGameState playerState = new PlayerGameState(playerId, playerName, characterImage);
            playerStates.put(playerId, playerState);
            connectedPlayers.add(playerId);
            
            Debug.log("Added player: " + playerName + " (" + playerId + ") with character: " + characterImage);
            updateGamePhase();
            return true;
        }
    }
    
    public synchronized boolean removePlayer(String playerId) {
        synchronized (stateLock) {
            if (playerStates.remove(playerId) != null) {
                connectedPlayers.remove(playerId);
                Debug.log("Removed player: " + playerId);
                
                if (currentTurnPlayer != null && currentTurnPlayer.equals(playerId)) {
                    nextTurn();
                }
                
                updateGamePhase();
                return true;
            }
            return false;
        }
    }
    
    public synchronized void updatePlayerData(String playerId, int money, int health, int energy, int remainingTime) {
        synchronized (stateLock) {
            PlayerGameState playerState = playerStates.get(playerId);
            if (playerState != null) {
                playerState.updateStats(money, health, energy, remainingTime);
                lastStateUpdate = System.currentTimeMillis();
            }
        }
    }
    
    public synchronized void updatePlayerPosition(String playerId, java.awt.Point position) {
        synchronized (stateLock) {
            PlayerGameState playerState = playerStates.get(playerId);
            if (playerState != null) {
                playerState.updatePosition(position);
            }
        }
    }
    
    public synchronized void nextTurn() {
        synchronized (stateLock) {
            if (connectedPlayers.isEmpty()) {
                currentTurnPlayer = null;
                return;
            }
            
            List<String> playerList = new ArrayList<>(connectedPlayers);
            java.util.Collections.sort(playerList);
            
            if (currentTurnPlayer == null) {
                currentTurnPlayer = playerList.get(0);
            } else {
                int currentIndex = playerList.indexOf(currentTurnPlayer);
                int nextIndex = (currentIndex + 1) % playerList.size();
                currentTurnPlayer = playerList.get(nextIndex);
            }
            
            Debug.log("Turn changed to: " + currentTurnPlayer);
        }
    }
    
    private synchronized void updateGamePhase() {
        synchronized (stateLock) {
            int playerCount = connectedPlayers.size();

            if (playerCount < GameConfig.Game.MIN_PLAYERS_TO_START) {
                if (currentPhase != GamePhase.WAITING_FOR_PLAYERS) {
                    currentPhase = GamePhase.WAITING_FOR_PLAYERS;
                    Debug.log("Game phase changed to: WAITING_FOR_PLAYERS (" + playerCount + "/" + GameConfig.Game.MIN_PLAYERS_TO_START + ")");
                }
            } else if (currentPhase == GamePhase.WAITING_FOR_PLAYERS) {
                currentPhase = GamePhase.GAME_STARTING;
                Debug.log("Game phase changed to: GAME_STARTING");
                
                if (currentTurnPlayer == null) {
                    nextTurn();
                }
                
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        synchronized (stateLock) {
                            if (currentPhase == GamePhase.GAME_STARTING) {
                                currentPhase = GamePhase.GAME_RUNNING;
                                Debug.log("Game phase changed to: GAME_RUNNING");
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        }
    }
    
    public synchronized boolean isGameReady() {
        synchronized (stateLock) {
            return currentPhase == GamePhase.GAME_RUNNING &&
                   connectedPlayers.size() >= GameConfig.Game.MIN_PLAYERS_TO_START;
        }
    }
    
    public synchronized boolean isWaitingForPlayers() {
        synchronized (stateLock) {
            return currentPhase == GamePhase.WAITING_FOR_PLAYERS;
        }
    }
    
    public synchronized boolean isPlayerTurn(String playerId) {
        synchronized (stateLock) {
            return currentTurnPlayer != null && currentTurnPlayer.equals(playerId);
        }
    }
    
    public synchronized Map<String, PlayerGameState> getAllPlayerStates() {
        synchronized (stateLock) {
            return new ConcurrentHashMap<>(playerStates);
        }
    }
    
    public synchronized int getConnectedPlayerCount() {
        synchronized (stateLock) {
            return connectedPlayers.size();
        }
    }
    
    public synchronized String getCurrentTurnPlayer() {
        synchronized (stateLock) {
            return currentTurnPlayer;
        }
    }
    
    public synchronized GamePhase getCurrentPhase() {
        synchronized (stateLock) {
            return currentPhase;
        }
    }
    
    public synchronized void resetGame() {
        synchronized (stateLock) {
            playerStates.clear();
            connectedPlayers.clear();
            currentTurnPlayer = null;
            currentPhase = GamePhase.WAITING_FOR_PLAYERS;
            Debug.log("Game state reset");
        }
    }
    
    public static class PlayerGameState {
        private final String playerId;
        private final String playerName;
        private final String characterImage;
        private int money;
        private int health;
        private int energy;
        private int remainingTime;
        private java.awt.Point position;
        private long lastUpdateTime;
        
        public PlayerGameState(String playerId, String playerName, String characterImage) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.characterImage = characterImage;
            this.money = GameConfig.Character.STARTING_MONEY;
            this.health = GameConfig.Character.STARTING_HEALTH;
            this.energy = GameConfig.Character.STARTING_ENERGY;
            this.remainingTime = GameConfig.Game.TURN_TIME_HOURS;
            this.position = GameConfig.Map.APARTMENT_POINT;
            this.lastUpdateTime = System.currentTimeMillis();
        }
        
        public void updateStats(int money, int health, int energy, int remainingTime) {
            this.money = money;
            this.health = health;
            this.energy = energy;
            this.remainingTime = remainingTime;
            this.lastUpdateTime = System.currentTimeMillis();
        }
        
        public void updatePosition(java.awt.Point position) {
            this.position = position;
            this.lastUpdateTime = System.currentTimeMillis();
        }
        
        public void updateCharacterImage(String characterImage) {
            Debug.log("Character image update requested for " + playerId + " to " + characterImage);
        }
        
        public String getPlayerId() { return playerId; }
        public String getPlayerName() { return playerName; }
        public String getCharacterImage() { return characterImage; }
        public int getMoney() { return money; }
        public int getHealth() { return health; }
        public int getEnergy() { return energy; }
        public int getRemainingTime() { return remainingTime; }
        public java.awt.Point getPosition() { return position; }
        public long getLastUpdateTime() { return lastUpdateTime; }
    }
}
