package network;

import java.awt.Point;
import java.io.Serializable;

public class NetworkMessage implements Serializable {
    public enum MessageType {
        PLAYER_JOIN,
        PLAYER_LEAVE,
        PLAYER_MOVE,
        PLAYER_UPDATE
    }
    
    public MessageType type;
    public String playerId;
    public String playerName;
    public Point position;
    public String characterImage;
    public long timestamp;
    
    public NetworkMessage(MessageType type, String playerId) {
        this.type = type;
        this.playerId = playerId;
        this.timestamp = System.currentTimeMillis();
    }
    
    public static NetworkMessage createPlayerJoin(String playerId, String playerName, Point position, String characterImage) {
        NetworkMessage msg = new NetworkMessage(MessageType.PLAYER_JOIN, playerId);
        msg.playerName = playerName;
        msg.position = position;
        msg.characterImage = characterImage;
        return msg;
    }
    
    public static NetworkMessage createPlayerMove(String playerId, Point position) {
        NetworkMessage msg = new NetworkMessage(MessageType.PLAYER_MOVE, playerId);
        msg.position = position;
        return msg;
    }
    
    public static NetworkMessage createPlayerUpdate(String playerId, String playerName, Point position, String characterImage) {
        NetworkMessage msg = new NetworkMessage(MessageType.PLAYER_UPDATE, playerId);
        msg.playerName = playerName;
        msg.position = position;
        msg.characterImage = characterImage;
        return msg;
    }
}
