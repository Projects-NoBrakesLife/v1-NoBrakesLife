package network;

import java.awt.Point;

public class OnlinePlayer {
    public String playerId;
    public String playerName;
    public Point position;
    public String characterImage;
    public boolean isActive;
    
    public OnlinePlayer(String playerId, String playerName, Point position, String characterImage) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.position = position;
        this.characterImage = characterImage;
        this.isActive = true;
    }
    
    public void updatePosition(Point newPosition) {
        this.position = newPosition;
    }
}
