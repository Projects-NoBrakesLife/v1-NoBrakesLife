package ui;

import core.PlayerState;
import java.awt.Dimension;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

public class OnlinePlayerHUDManager {
    private Map<String, OnlinePlayerHUD> onlineHUDs;
    private Map<String, PlayerState> playerStates;
    private Dimension screenSize;
    private int margin;
    
    public OnlinePlayerHUDManager(Dimension screenSize) {
        this.onlineHUDs = new HashMap<>();
        this.playerStates = new HashMap<>();
        this.screenSize = screenSize;
        this.margin = 20;
    }
    
    public void addPlayer(String playerId, PlayerState playerState) {
        if (onlineHUDs.containsKey(playerId)) {
            return;
        }
        
        if (onlineHUDs.size() >= 3) {
            return;
        }
        
        Point position = calculatePosition(onlineHUDs.size());
        boolean isRightSide = (onlineHUDs.size() == 0 || onlineHUDs.size() == 2);
        int playerNumber = onlineHUDs.size() + 2;
        OnlinePlayerHUD hud = new OnlinePlayerHUD(playerId, playerState, position.x, position.y, isRightSide, playerNumber);
        onlineHUDs.put(playerId, hud);
        playerStates.put(playerId, playerState);
    }
    
    public void removePlayer(String playerId) {
        onlineHUDs.remove(playerId);
        playerStates.remove(playerId);
        reorganizePositions();
    }
    
    public void updatePlayer(String playerId, network.OnlinePlayer player) {
        if (player == null || playerId == null) return;
        
        OnlinePlayerHUD hud = onlineHUDs.get(playerId);
        if (hud != null) {
            PlayerState existingState = playerStates.get(playerId);
            if (existingState != null) {
                existingState.setPlayerName(player.getPlayerName());
                existingState.setCharacterImagePath(player.getCharacterImage());
                existingState.setCurrentPosition(player.getPosition());
                existingState.setMoney(player.getMoney());
                existingState.setHealth(player.getHealth());
                existingState.setEnergy(player.getEnergy());
                hud.updatePlayerState(existingState);
            }
        } else if (player.getCharacterImage() != null && !player.getCharacterImage().isEmpty() && onlineHUDs.size() < 3) {
            PlayerState playerState = new PlayerState();
            playerState.setPlayerName(player.getPlayerName());
            playerState.setCharacterImagePath(player.getCharacterImage());
            playerState.setCurrentPosition(player.getPosition());
            playerState.setMoney(player.getMoney());
            playerState.setHealth(player.getHealth());
            playerState.setEnergy(player.getEnergy());
            addPlayer(playerId, playerState);
        }
    }
    
    private Point calculatePosition(int index) {
        int hudSize = 100;
        int spacing = 140;
        
        switch (index) {
            case 0:
                return new Point(screenSize.width - hudSize - margin - 20, margin + 50);
            case 1:
                return new Point(margin + 30, screenSize.height - hudSize - margin - 20);
            case 2:
                return new Point(screenSize.width - hudSize - margin - 20, screenSize.height - hudSize - margin - 20);
            case 3:
                return new Point(margin + spacing, margin + spacing);
            default:
                return new Point(margin + spacing, margin + spacing);
        }
    }
    
    private void reorganizePositions() {
        int index = 0;
        for (OnlinePlayerHUD hud : onlineHUDs.values()) {
            Point newPosition = calculatePosition(index);
            boolean isRightSide = (index == 0 || index == 2);
            hud.setPosition(newPosition.x, newPosition.y);
            hud.setPlayerNumber(index + 2);
            index++;
        }
    }
    
    public void updateTurn(String currentTurnPlayerId) {
        for (Map.Entry<String, OnlinePlayerHUD> entry : onlineHUDs.entrySet()) {
            String playerId = entry.getKey();
            OnlinePlayerHUD hud = entry.getValue();
            hud.setCurrentTurn(playerId.equals(currentTurnPlayerId));
        }
    }
    
    public void drawAll(java.awt.Graphics2D g2d) {
        for (OnlinePlayerHUD hud : onlineHUDs.values()) {
            if (hud.hasCharacterIcon()) {
                hud.draw(g2d);
            }
        }
    }
    
    public void setPlayerNumber(String playerId, int playerNumber) {
        OnlinePlayerHUD hud = onlineHUDs.get(playerId);
        if (hud != null) {
            hud.setPlayerNumber(playerNumber);
        }
    }
    
    public void updateScreenSize(Dimension newSize) {
        this.screenSize = newSize;
        reorganizePositions();
    }
}