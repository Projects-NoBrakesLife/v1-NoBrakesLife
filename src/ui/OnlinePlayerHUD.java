package ui;

import core.PlayerState;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import util.FontManager;

public class OnlinePlayerHUD {
    private BufferedImage characterIcon;
    private BufferedImage educationIcon;
    private BufferedImage happinessIcon;
    private BufferedImage healthIcon;
    private BufferedImage moneyIcon;
    private BufferedImage tokenIcon;
    private PlayerState playerState;
    private String playerId;
    private int x, y;
    private int mainCircleRadius;
    private int iconRadius;
    private Font hudFont;
    private Color backgroundColor;
    private Color borderColor;
    private Color textColor;
    private boolean isRightSide;
    private int playerNumber;
    
    private boolean isCurrentTurn = false;
    
    public OnlinePlayerHUD(String playerId, PlayerState playerState, int x, int y, boolean isRightSide, int playerNumber) {
        this.playerId = playerId;
        this.playerState = playerState;
        this.x = x;
        this.y = y;
        this.isRightSide = isRightSide;
        this.playerNumber = playerNumber;
        this.mainCircleRadius = 40;
        this.iconRadius = 16;
        this.hudFont = FontManager.getFontForText("HUD", 10, Font.BOLD);
        this.backgroundColor = new Color(0, 0, 0, 120);
        this.borderColor = new Color(255, 255, 255, 180);
        this.textColor = Color.WHITE;
        loadIcons();
    }
    
    private void loadIcons() {
        try {
            educationIcon = ImageIO.read(new File("assets/ui/hud/Icon Education #125.png"));
            happinessIcon = ImageIO.read(new File("assets/ui/hud/Icon Happiness #6686.png"));
            healthIcon = ImageIO.read(new File("assets/ui/hud/Icon Health.png"));
            moneyIcon = ImageIO.read(new File("assets/ui/hud/Icon Money #6805.png"));
            loadTokenIcon();
            loadCharacterIcon();
        } catch (IOException e) {
            System.out.println("Could not load HUD icons for " + playerId + ": " + e.getMessage());
            createFallbackIcons();
        }
    }
    
    private void loadTokenIcon() {
        try {
            String tokenPath = "assets/ui/hud/Token P" + playerNumber + " Front.png";
            tokenIcon = ImageIO.read(new File(tokenPath));
        } catch (IOException e) {
            System.out.println("Could not load token icon for " + playerId + ": " + e.getMessage());
            tokenIcon = null;
        }
    }
    
    private void loadCharacterIcon() {
        try {
            String characterImagePath = playerState.getCharacterImagePath();
            if (characterImagePath != null && !characterImagePath.isEmpty()) {
                characterIcon = ImageIO.read(new File(characterImagePath));
            } else {
                characterIcon = null;
            }
        } catch (IOException e) {
            System.out.println("Could not load character icon for " + playerId + ": " + e.getMessage());
            characterIcon = null;
        }
    }
    
    private void createFallbackIcons() {
        int iconSize = 16;
        educationIcon = createFallbackIcon(iconSize, "E", new Color(100, 150, 255));
        happinessIcon = createFallbackIcon(iconSize, "H", new Color(255, 200, 100));
        healthIcon = createFallbackIcon(iconSize, "♥", new Color(255, 100, 100));
        moneyIcon = createFallbackIcon(iconSize, "$", new Color(100, 255, 100));
    }
    
    private BufferedImage createFallbackIcon(int size, String text, Color bgColor) {
        BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(bgColor);
        g2d.fillOval(0, 0, size, size);
        
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 8));
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (size - fm.stringWidth(text)) / 2;
        int textY = (size + fm.getAscent()) / 2;
        g2d.drawString(text, textX, textY);
        
        g2d.dispose();
        return icon;
    }
    
    public void draw(Graphics2D g2d) {
        if (characterIcon == null) {
            return;
        }
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (isCurrentTurn) {
            drawTurnIndicator(g2d);
        }
        
        drawMainCharacterCircle(g2d);
        drawStatusIcons(g2d);
        drawTokenIcon(g2d);
    }
    
    private void drawMainCharacterCircle(Graphics2D g2d) {
        if (characterIcon != null) {
            int charIconSize = mainCircleRadius * 2;
            g2d.drawImage(characterIcon, x - charIconSize/2, y - charIconSize/2, charIconSize, charIconSize, null);
        } else {
            g2d.setColor(textColor);
            g2d.setFont(FontManager.getFontForText("CHAR", 10, Font.BOLD));
            FontMetrics fm = g2d.getFontMetrics();
            String charText = "CHAR";
            int textX = x - fm.stringWidth(charText) / 2;
            int textY = y + fm.getAscent() / 2;
            g2d.drawString(charText, textX, textY);
        }
    }
    
    private void drawStatusIcons(Graphics2D g2d) {
        int iconDistance = mainCircleRadius + iconRadius - 6;
        int rightSideX = x + mainCircleRadius + iconRadius - 6;
        
        drawStatusIcon(g2d, educationIcon, rightSideX, y - iconDistance, "Education", calculateEducationProgress());
        drawStatusIcon(g2d, happinessIcon, rightSideX + iconRadius + 2, y - iconDistance/2, "Happiness", calculateHappinessProgress());
        drawStatusIcon(g2d, healthIcon, rightSideX + iconRadius + 2, y + iconDistance/2, "Health", playerState.getHealth());
        drawStatusIcon(g2d, moneyIcon, rightSideX, y + iconDistance, "Money", calculateMoneyProgress());
    }
    
    private void drawStatusIcon(Graphics2D g2d, BufferedImage icon, int centerX, int centerY, String label, int progress) {
        if (icon != null) {
            int iconSize = iconRadius * 2;
            g2d.drawImage(icon, centerX - iconSize/2, centerY - iconSize/2, iconSize, iconSize, null);
        }
        
        drawValueText(g2d, centerX + iconRadius + 8, centerY, progress);
    }
    
    private void drawValueText(Graphics2D g2d, int x, int y, int value) {
        String valueText = String.valueOf(value);
        Font valueFont = FontManager.getFontForText("Value", 10, Font.BOLD);
        FontMetrics fm = g2d.getFontMetrics(valueFont);
        
        int textWidth = fm.stringWidth(valueText);
        int textHeight = fm.getHeight();
        int padding = 3;
        
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(x - padding, y - textHeight + padding, textWidth + padding * 2, textHeight + padding, 3, 3);
        
        g2d.setColor(textColor);
        g2d.setFont(valueFont);
        g2d.drawString(valueText, x, y);
    }
    
    private int calculateEducationProgress() {
        return Math.min(100, (playerState.getEnergy() * 2));
    }
    
    private int calculateHappinessProgress() {
        return Math.min(100, (playerState.getHealth() + playerState.getEnergy()) / 2);
    }
    
    private int calculateMoneyProgress() {
        return Math.min(100, playerState.getMoney() / 10);
    }
    
    private void drawTokenIcon(Graphics2D g2d) {
        if (tokenIcon != null) {
            int tokenSize = 20;
            int tokenX = x - tokenSize/2;
            int tokenY = y + mainCircleRadius + 8;
            g2d.drawImage(tokenIcon, tokenX, tokenY, tokenSize, tokenSize, null);
        }
    }
    
    private void drawTurnIndicator(Graphics2D g2d) {
        int indicatorSize = mainCircleRadius + 10;
        g2d.setColor(new Color(255, 255, 0, 150));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawOval(x - indicatorSize/2, y - indicatorSize/2, indicatorSize, indicatorSize);
        
        if (tokenIcon != null) {
            int tokenSize = 24;
            int tokenX = x - tokenSize/2;
            int tokenY = y + mainCircleRadius + 5;
            g2d.drawImage(tokenIcon, tokenX, tokenY, tokenSize, tokenSize, null);
        } else {
            g2d.setColor(Color.YELLOW);
            g2d.setFont(FontManager.getFontForText("TURN", 8, Font.BOLD));
            FontMetrics fm = g2d.getFontMetrics();
            String turnText = "TURN";
            int textX = x - fm.stringWidth(turnText) / 2;
            int textY = y + mainCircleRadius + 15;
            g2d.drawString(turnText, textX, textY);
        }
    }
    
    public void setCurrentTurn(boolean isCurrentTurn) {
        this.isCurrentTurn = isCurrentTurn;
    }
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public void setPlayerNumber(int playerNumber) {
        this.playerNumber = playerNumber;
        loadTokenIcon();
    }
    
    public void updatePlayerState(PlayerState newState) {
        this.playerState = newState;
        loadCharacterIcon();
    }
    
    public String getPlayerId() {
        return playerId;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public boolean hasCharacterIcon() {
        return characterIcon != null;
    }
}