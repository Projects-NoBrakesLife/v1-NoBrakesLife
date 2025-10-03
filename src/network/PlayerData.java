package network;

import core.PlayerState;
import java.awt.Point;
import java.io.Serializable;

public class PlayerData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public String playerId;
    public String playerName;
    public Point position;
    public String characterImage;
    public int money;
    public int health;
    public int energy;
    public PlayerState.Location currentLocation;
    public long timestamp;
    
    public PlayerData(String playerId, String playerName, Point position, String characterImage) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.position = new Point(position);
        this.characterImage = characterImage;
        this.money = 1000;
        this.health = 100;
        this.energy = 100;
        this.currentLocation = PlayerState.Location.APARTMENT_SHITTY;
        this.timestamp = System.currentTimeMillis();
    }
    
    public PlayerData(String playerId, String playerName, Point position, String characterImage, 
                     int money, int health, int energy, PlayerState.Location location) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.position = new Point(position);
        this.characterImage = characterImage;
        this.money = money;
        this.health = health;
        this.energy = energy;
        this.currentLocation = location;
        this.timestamp = System.currentTimeMillis();
    }
    
    public void updatePosition(Point newPosition) {
        this.position = new Point(newPosition);
        this.timestamp = System.currentTimeMillis();
    }
    
    public void updateStats(int money, int health, int energy) {
        this.money = money;
        this.health = health;
        this.energy = energy;
        this.timestamp = System.currentTimeMillis();
    }
    
    public void updateLocation(PlayerState.Location location) {
        this.currentLocation = location;
        this.timestamp = System.currentTimeMillis();
    }
    
    public void updateCharacter(String characterImage) {
        this.characterImage = characterImage;
        this.timestamp = System.currentTimeMillis();
    }
    
    public PlayerData copy() {
        return new PlayerData(playerId, playerName, position, characterImage, 
                            money, health, energy, currentLocation);
    }
}
