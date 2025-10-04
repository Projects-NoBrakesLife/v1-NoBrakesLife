package core;

import java.awt.Point;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StableDataSyncManager implements DataSyncManager {
    
    private static final int STATS_SYNC_THRESHOLD = 800;
    private static final int POSITION_SYNC_THRESHOLD = 300;
    private static final int TIME_SYNC_THRESHOLD = 1500;
    private static final int LOCATION_SYNC_THRESHOLD = 1500;
    
    private final Map<String, PlayerSyncData> playerSyncData = new ConcurrentHashMap<>();
    
    private static StableDataSyncManager instance;
    
    public static StableDataSyncManager getInstance() {
        if (instance == null) {
            instance = new StableDataSyncManager();
        }
        return instance;
    }
    
    private StableDataSyncManager() {
        Debug.log("StableDataSyncManager initialized");
    }
    
    @Override
    public boolean shouldSyncStats(String playerId, int money, int health, int energy) {
        PlayerSyncData data = playerSyncData.get(playerId);
        if (data == null) {
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        boolean timeThreshold = (currentTime - data.lastStatsSyncTime) >= STATS_SYNC_THRESHOLD;
        boolean dataChanged = data.lastMoney != money || data.lastHealth != health || data.lastEnergy != energy;
        
        return timeThreshold && dataChanged;
    }
    
    @Override
    public boolean shouldSyncPosition(String playerId, Point position) {
        PlayerSyncData data = playerSyncData.get(playerId);
        if (data == null) {
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        boolean timeThreshold = (currentTime - data.lastPositionSyncTime) >= POSITION_SYNC_THRESHOLD;
        boolean positionChanged = data.lastPosition == null || !data.lastPosition.equals(position);
        
        return timeThreshold && positionChanged;
    }
    
    @Override
    public boolean shouldSyncTime(String playerId, int remainingTime) {
        PlayerSyncData data = playerSyncData.get(playerId);
        if (data == null) {
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        boolean timeThreshold = (currentTime - data.lastTimeSyncTime) >= TIME_SYNC_THRESHOLD;
        boolean timeChanged = data.lastRemainingTime != remainingTime;
        
        return timeThreshold && timeChanged;
    }
    
    @Override
    public boolean shouldSyncLocation(String playerId, PlayerState.Location location) {
        PlayerSyncData data = playerSyncData.get(playerId);
        if (data == null) {
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        boolean timeThreshold = (currentTime - data.lastLocationSyncTime) >= LOCATION_SYNC_THRESHOLD;
        boolean locationChanged = data.lastLocation != location;
        
        return timeThreshold && locationChanged;
    }
    
    @Override
    public void updateLastStatsSync(String playerId, int money, int health, int energy) {
        PlayerSyncData data = playerSyncData.computeIfAbsent(playerId, k -> new PlayerSyncData());
        data.lastMoney = money;
        data.lastHealth = health;
        data.lastEnergy = energy;
        data.lastStatsSyncTime = System.currentTimeMillis();
    }
    
    @Override
    public void updateLastPositionSync(String playerId, Point position) {
        PlayerSyncData data = playerSyncData.computeIfAbsent(playerId, k -> new PlayerSyncData());
        data.lastPosition = new Point(position);
        data.lastPositionSyncTime = System.currentTimeMillis();
    }
    
    @Override
    public void updateLastTimeSync(String playerId, int remainingTime) {
        PlayerSyncData data = playerSyncData.computeIfAbsent(playerId, k -> new PlayerSyncData());
        data.lastRemainingTime = remainingTime;
        data.lastTimeSyncTime = System.currentTimeMillis();
    }
    
    @Override
    public void updateLastLocationSync(String playerId, PlayerState.Location location) {
        PlayerSyncData data = playerSyncData.computeIfAbsent(playerId, k -> new PlayerSyncData());
        data.lastLocation = location;
        data.lastLocationSyncTime = System.currentTimeMillis();
    }
    
    @Override
    public void removePlayer(String playerId) {
        playerSyncData.remove(playerId);
        Debug.log("Removed sync data for player: " + playerId);
    }
    
    @Override
    public void reset() {
        playerSyncData.clear();
        Debug.log("Sync data reset");
    }
    
    private static class PlayerSyncData {
        int lastMoney = -1;
        int lastHealth = -1;
        int lastEnergy = -1;
        int lastRemainingTime = -1;
        Point lastPosition = null;
        PlayerState.Location lastLocation = null;
        
        long lastStatsSyncTime = 0;
        long lastPositionSyncTime = 0;
        long lastTimeSyncTime = 0;
        long lastLocationSyncTime = 0;
    }
}
