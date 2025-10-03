package network;

import core.PlayerState;
import java.awt.Point;

public interface OnlineDataManager {
    void updatePlayerPosition(String playerId, Point position);
    void updatePlayerLocation(String playerId, PlayerState.Location location);
    void updatePlayerStats(String playerId, int money, int health, int energy);
    boolean shouldUpdatePosition(String playerId, Point newPosition);
    boolean shouldUpdateLocation(String playerId, PlayerState.Location newLocation);
    boolean shouldUpdateStats(String playerId, int money, int health, int energy);
    void removePlayer(String playerId);
    void clearAll();
}
