package network;

import core.PlayerState;
import java.awt.Point;
import java.io.Serializable;

public class OnlinePlayer implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private PlayerData playerData;
    private boolean isActive;
    private long lastSeen;
    
    public OnlinePlayer(PlayerData playerData) {
        this.playerData = playerData.copy();
        this.isActive = true;
        this.lastSeen = System.currentTimeMillis();
    }
    
    public OnlinePlayer(String playerId, String playerName, Point position, String characterImage) {
        this.playerData = new PlayerData(playerId, playerName, position, characterImage);
        this.isActive = true;
        this.lastSeen = System.currentTimeMillis();
    }
    
    public String getPlayerId() {
        return playerData.playerId;
    }
    
    public String getPlayerName() {
        return playerData.playerName;
    }
    
    public Point getPosition() {
        return new Point(playerData.position);
    }
    
    public String getCharacterImage() {
        return playerData.characterImage;
    }
    
    public int getMoney() {
        return playerData.money;
    }
    
    public int getHealth() {
        return playerData.health;
    }
    
    public int getEnergy() {
        return playerData.energy;
    }
    
    public int getRemainingTime() {
        return playerData.remainingTime;
    }
    
    public PlayerState.Location getCurrentLocation() {
        return playerData.currentLocation;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public long getLastSeen() {
        return lastSeen;
    }
    
    public void updatePosition(Point newPosition) {
        playerData.updatePosition(newPosition);
        updateLastSeen();
    }
    
    public void updateStats(int money, int health, int energy) {
        playerData.updateStats(money, health, energy);
        updateLastSeen();
    }
    
    public void updateLocation(PlayerState.Location location) {
        playerData.updateLocation(location);
        updateLastSeen();
    }
    
    public void updateCharacter(String characterImage) {
        playerData.updateCharacter(characterImage);
        updateLastSeen();
    }
    
    public void updateTime(int remainingTime) {
        playerData.updateTime(remainingTime);
        updateLastSeen();
    }
    
    public void updateFromPlayerData(PlayerData newData) {
        this.playerData = newData.copy();
        updateLastSeen();
    }
    
    public PlayerData getPlayerData() {
        return playerData.copy();
    }
    
    public void setActive(boolean active) {
        this.isActive = active;
        if (active) {
            updateLastSeen();
        }
    }
    
    private void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }
}
