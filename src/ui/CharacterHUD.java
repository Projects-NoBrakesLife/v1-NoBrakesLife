package ui;

import core.Lang;
import core.PlayerState;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class CharacterHUD {
    private BufferedImage educationIcon;
    private BufferedImage happinessIcon;
    private BufferedImage healthIcon;
    private BufferedImage moneyIcon;
    private BufferedImage characterIcon;
    private BufferedImage tokenIcon;
    private BufferedImage tokenBackIcon;
    private int playerNumber = 0;
    private PlayerState playerState;
    private int x, y;
    private int mainCircleRadius;
    private int iconRadius;
    private Font hudFont;
    private Color backgroundColor;
    private Color borderColor;
    private Color textColor;
    
    public CharacterHUD(PlayerState playerState, int x, int y) {
        this.playerState = playerState;
        this.x = x;
        this.y = y;
        this.playerNumber = 0;
        this.mainCircleRadius = 50;
        this.iconRadius = 20;
        this.hudFont = new Font("Arial", Font.BOLD, 9);
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
            System.out.println("Could not load HUD icons: " + e.getMessage());
            createFallbackIcons();
        }
    }
    
    private void loadTokenIcon() {
        try {
            String tokenFrontPath = "assets/ui/hud/Token P" + playerNumber + " Front.png";
            String tokenBackPath = "assets/ui/hud/P" + playerNumber + " Back.png";
            tokenIcon = ImageIO.read(new File(tokenFrontPath));
            tokenBackIcon = ImageIO.read(new File(tokenBackPath));
        } catch (IOException e) {
            System.out.println("Could not load token icon: " + e.getMessage());
            tokenIcon = null;
            tokenBackIcon = null;
        }
    }
    
    private void loadCharacterIcon() {
        try {
            String characterImagePath = playerState.getCharacterImagePath();
            if (characterImagePath != null && !characterImagePath.isEmpty()) {
                characterIcon = ImageIO.read(new File(characterImagePath));
            } else {
                characterIcon = ImageIO.read(new File(Lang.MALE_01));
            }
        } catch (IOException e) {
            System.out.println("Could not load character icon: " + e.getMessage());
            characterIcon = null;
        }
    }
    
    private void createFallbackIcons() {
        int iconSize = 20;
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
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (size - fm.stringWidth(text)) / 2;
        int textY = (size + fm.getAscent()) / 2;
        g2d.drawString(text, textX, textY);
        
        g2d.dispose();
        return icon;
    }
    
    public void draw(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
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
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g2d.getFontMetrics();
            String charText = "CHAR";
            int textX = x - fm.stringWidth(charText) / 2;
            int textY = y + fm.getAscent() / 2;
            g2d.drawString(charText, textX, textY);
        }
    }
    
    private void drawStatusIcons(Graphics2D g2d) {
        int iconDistance = mainCircleRadius + iconRadius - 8;
        int rightSideX = x + mainCircleRadius + iconRadius - 3;
        
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
        Font valueFont = new Font("Arial", Font.BOLD, 14);
        FontMetrics fm = g2d.getFontMetrics(valueFont);
        
        int textWidth = fm.stringWidth(valueText);
        int textHeight = fm.getHeight();
        int padding = 4;
        
        g2d.setColor(new Color(0, 0, 0, 150));
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
        BufferedImage currentTokenIcon = isCurrentTurn ? tokenBackIcon : tokenIcon;
        if (currentTokenIcon != null) {
            int tokenSize = isCurrentTurn ? 30 : 20;
            int tokenX = x - tokenSize/2;
            int tokenY = y + mainCircleRadius + 8;
            g2d.drawImage(currentTokenIcon, tokenX, tokenY, tokenSize, tokenSize, null);
        }
    }
    
    private boolean isCurrentTurn = false;
    
    public void setPlayerNumber(int playerNumber) {
        this.playerNumber = playerNumber;
        loadTokenIcon();
    }
    
    public void setCurrentTurn(boolean isCurrentTurn) {
        this.isCurrentTurn = isCurrentTurn;
    }
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public void updatePlayerState(PlayerState newState) {
        this.playerState = newState;
        loadCharacterIcon();
    }
}
