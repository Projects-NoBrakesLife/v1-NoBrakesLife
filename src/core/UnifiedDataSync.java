package core;

import java.awt.Point;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UnifiedDataSync {
    private static UnifiedDataSync instance;
    private final Map<String, PlayerSyncData> playerSyncData = new ConcurrentHashMap<>();
    
    private static final int UNIFIED_SYNC_INTERVAL = 200;
    private static final int POSITION_CHANGE_THRESHOLD = 2;
    private static final int STATS_CHANGE_THRESHOLD = 1;
    
    public static UnifiedDataSync getInstance() {
        if (instance == null) {
            instance = new UnifiedDataSync();
        }
        return instance;
    }
    
    private UnifiedDataSync() {
    }
    
    public boolean shouldSyncPlayerData(String playerId, Point position, int money, int health, int energy, int remainingTime) {
        PlayerSyncData data = playerSyncData.get(playerId);
        if (data == null) {
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        boolean timeThreshold = (currentTime - data.lastSyncTime) >= UNIFIED_SYNC_INTERVAL;
        
        boolean positionChanged = data.lastPosition == null || 
            Math.abs(data.lastPosition.x - position.x) >= POSITION_CHANGE_THRESHOLD ||
            Math.abs(data.lastPosition.y - position.y) >= POSITION_CHANGE_THRESHOLD;
        
        boolean statsChanged = data.lastMoney != money || 
            data.lastHealth != health || 
            data.lastEnergy != energy ||
            data.lastRemainingTime != remainingTime;
        
        return timeThreshold && (positionChanged || statsChanged);
    }
    
    public void updateLastSync(String playerId, Point position, int money, int health, int energy, int remainingTime) {
        PlayerSyncData data = playerSyncData.computeIfAbsent(playerId, k -> new PlayerSyncData());
        data.lastPosition = new Point(position);
        data.lastMoney = money;
        data.lastHealth = health;
        data.lastEnergy = energy;
        data.lastRemainingTime = remainingTime;
        data.lastSyncTime = System.currentTimeMillis();
    }
    
    public void removePlayer(String playerId) {
        playerSyncData.remove(playerId);
    }
    
    public void reset() {
        playerSyncData.clear();
    }
    
    private static class PlayerSyncData {
        Point lastPosition = null;
        int lastMoney = -1;
        int lastHealth = -1;
        int lastEnergy = -1;
        int lastRemainingTime = -1;
        long lastSyncTime = 0;
    }
}
