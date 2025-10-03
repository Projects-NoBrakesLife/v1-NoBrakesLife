package network;

import core.PlayerState;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

public class OnlineDataManagerImpl implements OnlineDataManager {
    private Map<String, Point> lastPositions = new HashMap<>();
    private Map<String, Long> lastPositionTimes = new HashMap<>();
    private Map<String, PlayerState.Location> lastLocations = new HashMap<>();
    private Map<String, Long> lastLocationTimes = new HashMap<>();
    private Map<String, Integer> lastMoney = new HashMap<>();
    private Map<String, Integer> lastHealth = new HashMap<>();
    private Map<String, Integer> lastEnergy = new HashMap<>();
    private Map<String, Long> lastStatsTimes = new HashMap<>();
    
    private static final long POSITION_UPDATE_INTERVAL = 1000;
    private static final long LOCATION_UPDATE_INTERVAL = 3000;
    private static final long STATS_UPDATE_INTERVAL = 5000;
    private static final int POSITION_THRESHOLD = 30;
    
    @Override
    public void updatePlayerPosition(String playerId, Point position) {
        lastPositions.put(playerId, new Point(position));
        lastPositionTimes.put(playerId, System.currentTimeMillis());
    }
    
    @Override
    public void updatePlayerLocation(String playerId, PlayerState.Location location) {
        lastLocations.put(playerId, location);
        lastLocationTimes.put(playerId, System.currentTimeMillis());
    }
    
    @Override
    public void updatePlayerStats(String playerId, int money, int health, int energy) {
        lastMoney.put(playerId, money);
        lastHealth.put(playerId, health);
        lastEnergy.put(playerId, energy);
        lastStatsTimes.put(playerId, System.currentTimeMillis());
    }
    
    @Override
    public boolean shouldUpdatePosition(String playerId, Point newPosition) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastPositionTimes.get(playerId);
        Point lastPos = lastPositions.get(playerId);
        
        if (lastTime == null || lastPos == null) {
            return true;
        }
        
        boolean timeElapsed = (currentTime - lastTime) > POSITION_UPDATE_INTERVAL;
        boolean significantMove = lastPos.distance(newPosition) > POSITION_THRESHOLD;
        
        return timeElapsed || significantMove;
    }
    
    @Override
    public boolean shouldUpdateLocation(String playerId, PlayerState.Location newLocation) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastLocationTimes.get(playerId);
        PlayerState.Location lastLoc = lastLocations.get(playerId);
        
        if (lastTime == null || lastLoc == null) {
            return true;
        }
        
        boolean timeElapsed = (currentTime - lastTime) > LOCATION_UPDATE_INTERVAL;
        boolean locationChanged = lastLoc != newLocation;
        
        return timeElapsed || locationChanged;
    }
    
    @Override
    public boolean shouldUpdateStats(String playerId, int money, int health, int energy) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastStatsTimes.get(playerId);
        Integer lastMoneyValue = lastMoney.get(playerId);
        Integer lastHealthValue = lastHealth.get(playerId);
        Integer lastEnergyValue = lastEnergy.get(playerId);
        
        if (lastTime == null || lastMoneyValue == null || lastHealthValue == null || lastEnergyValue == null) {
            return true;
        }
        
        boolean timeElapsed = (currentTime - lastTime) > STATS_UPDATE_INTERVAL;
        boolean statsChanged = (lastMoneyValue != money || lastHealthValue != health || lastEnergyValue != energy);
        
        return timeElapsed || statsChanged;
    }
    
    @Override
    public void removePlayer(String playerId) {
        lastPositions.remove(playerId);
        lastPositionTimes.remove(playerId);
        lastLocations.remove(playerId);
        lastLocationTimes.remove(playerId);
        lastMoney.remove(playerId);
        lastHealth.remove(playerId);
        lastEnergy.remove(playerId);
        lastStatsTimes.remove(playerId);
    }
    
    @Override
    public void clearAll() {
        lastPositions.clear();
        lastPositionTimes.clear();
        lastLocations.clear();
        lastLocationTimes.clear();
        lastMoney.clear();
        lastHealth.clear();
        lastEnergy.clear();
        lastStatsTimes.clear();
    }
}
