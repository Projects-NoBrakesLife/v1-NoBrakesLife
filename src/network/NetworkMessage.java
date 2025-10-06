package network;

import core.PlayerState;
import java.awt.Point;
import java.io.Serializable;

public class NetworkMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum MessageType {
        PLAYER_JOIN,
        PLAYER_LEAVE,
        PLAYER_MOVE,
        PLAYER_UPDATE,
        PLAYER_STATS_UPDATE,
        PLAYER_LOCATION_CHANGE,
        PLAYER_TIME_UPDATE,
        TURN_COMPLETE,
        TURN_CHANGE,
        GAME_STATE_UPDATE,
        HEARTBEAT
    }
    
    public MessageType type;
    public PlayerData playerData;
    public long timestamp;
    
    public NetworkMessage(MessageType type, PlayerData playerData) {
        this.type = type;
        this.playerData = playerData;
        this.timestamp = System.currentTimeMillis();
    }
    
    public static NetworkMessage createPlayerJoin(PlayerData playerData) {
        return new NetworkMessage(MessageType.PLAYER_JOIN, playerData);
    }
    
    public static NetworkMessage createPlayerMove(String playerId, Point position) {
        PlayerData data = new PlayerData(playerId, "", position, "");
        data.updatePosition(position);
        return new NetworkMessage(MessageType.PLAYER_MOVE, data);
    }
    
    public static NetworkMessage createPlayerUpdate(PlayerData playerData) {
        return new NetworkMessage(MessageType.PLAYER_UPDATE, playerData);
    }
    
    public static NetworkMessage createPlayerStatsUpdate(String playerId, int money, int health, int energy) {
        PlayerData data = new PlayerData(playerId, "", new Point(), "");
        data.updateStats(money, health, energy);
        return new NetworkMessage(MessageType.PLAYER_STATS_UPDATE, data);
    }
    
    public static NetworkMessage createPlayerLocationChange(String playerId, PlayerState.Location location) {
        PlayerData data = new PlayerData(playerId, "", new Point(), "");
        data.updateLocation(location);
        return new NetworkMessage(MessageType.PLAYER_LOCATION_CHANGE, data);
    }
    
    public static NetworkMessage createPlayerTimeUpdate(String playerId, int remainingTime) {
        PlayerData data = new PlayerData(playerId, "", new Point(), "");
        data.updateTime(remainingTime);
        return new NetworkMessage(MessageType.PLAYER_TIME_UPDATE, data);
    }
    
    public static NetworkMessage createTurnComplete(String playerId) {
        PlayerData data = new PlayerData(playerId, "", new Point(), "");
        return new NetworkMessage(MessageType.TURN_COMPLETE, data);
    }
    
    public static NetworkMessage createGameStateUpdate(int playerCount, boolean gameStarted, String currentTurnPlayer) {
        PlayerData data = new PlayerData("", "", new Point(), "");
        data.playerCount = playerCount;
        data.gameStarted = gameStarted;
        data.currentTurnPlayer = currentTurnPlayer;
        return new NetworkMessage(MessageType.GAME_STATE_UPDATE, data);
    }
    
    public static NetworkMessage createTurnChange(String currentTurnPlayerId) {
        PlayerData data = new PlayerData(currentTurnPlayerId, "", new Point(), "");
        return new NetworkMessage(MessageType.TURN_CHANGE, data);
    }

    public static NetworkMessage createHeartbeat(String playerId) {
        PlayerData data = new PlayerData(playerId, "", new Point(), "");
        return new NetworkMessage(MessageType.HEARTBEAT, data);
    }

    public static NetworkMessage createPlayerLeave(String playerId) {
        PlayerData data = new PlayerData(playerId, "", new Point(), "");
        return new NetworkMessage(MessageType.PLAYER_LEAVE, data);
    }
}
