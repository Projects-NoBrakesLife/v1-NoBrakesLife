package core;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Character {
    private BufferedImage characterImage;
    private Point position;
    private int width = 64;
    private int height = 64;
    private String imagePath;
    
    public Character(Point startPosition) {
        this.position = new Point(startPosition);
        
        String[] characterImages = {"./assets/players/Male-01.png", "./assets/players/Male-02.png"};
        this.imagePath = characterImages[(int)(Math.random() * characterImages.length)];
        
        try {
            characterImage = ImageIO.read(new File(imagePath));
            System.out.println("Character image loaded successfully: " + imagePath);
        } catch (IOException e) {
            System.out.println("Could not load character image: " + e.getMessage());
        }
        
        System.out.println("Character created at: " + startPosition);
    }
    
    public Character(Point startPosition, String imagePath) {
        this.position = new Point(startPosition);
        this.imagePath = imagePath;
        
        try {
            characterImage = ImageIO.read(new File(imagePath));
            System.out.println("Character image loaded successfully: " + imagePath);
        } catch (IOException e) {
            System.out.println("Could not load character image: " + e.getMessage());
        }
        
        System.out.println("Character created at: " + startPosition);
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public void setPosition(Point newPosition) {
        this.position = new Point(newPosition);
        System.out.println("Character position set to: " + position);
    }
    
    public Point getPosition() {
        return new Point(position);
    }
    
    public void draw(Graphics2D g2d) {
        if (characterImage != null) {
            g2d.drawImage(characterImage, position.x - width/2, position.y - height/2, width, height, null);
        } else {
            g2d.setColor(Color.RED);
            g2d.fillOval(position.x - width/2, position.y - height/2, width, height);
            g2d.setColor(Color.WHITE);
            g2d.drawString("NO IMAGE", position.x - 20, position.y);
        }
    }
    
    public boolean isMoving() {
        return false;
    }
    
    public Point getCurrentPosition() {
        return new Point(position);
    }
}
