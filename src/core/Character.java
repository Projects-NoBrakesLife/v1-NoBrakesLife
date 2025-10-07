package core;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Character {
    private BufferedImage characterImage;
    private Point position;
    private int width = GameConfig.Character.WIDTH;
    private int height = GameConfig.Character.HEIGHT;
    private String imagePath;
    
    public Character(Point startPosition) {
        this.position = new Point(startPosition);
        
        String[] characterImages = {Lang.MALE_01, Lang.MALE_02, Lang.FEMALE_01, Lang.FEMALE_02};
        this.imagePath = characterImages[(int)(Math.random() * characterImages.length)];
        
        try {
            characterImage = ImageIO.read(new File(imagePath));
            Debug.log(Lang.CHARACTER_IMAGE_LOADED + imagePath);
        } catch (IOException e) {
            Debug.error(Lang.CHARACTER_IMAGE_ERROR + e.getMessage());
        }
        
        Debug.log(Lang.CHARACTER_CREATED_AT + startPosition);
    }
    
    public Character(Point startPosition, String imagePath) {
        this.position = new Point(startPosition);
        this.imagePath = imagePath;
        
        try {
            characterImage = ImageIO.read(new File(imagePath));
            Debug.log(Lang.CHARACTER_IMAGE_LOADED + imagePath);
        } catch (IOException e) {
            Debug.error(Lang.CHARACTER_IMAGE_ERROR + e.getMessage());
        }
        
        Debug.log(Lang.CHARACTER_CREATED_AT + startPosition);
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public void setPosition(Point newPosition) {
        this.position = new Point(newPosition);
        Debug.log(Lang.CHARACTER_POSITION_SET + position);
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
            g2d.drawString(Lang.NO_IMAGE, position.x - 20, position.y);
        }
    }
    
    public void updateImage(String newImagePath) {
        if (!newImagePath.equals(this.imagePath)) {
            this.imagePath = newImagePath;
            try {
                characterImage = ImageIO.read(new File(imagePath));
                Debug.log(Lang.CHARACTER_IMAGE_UPDATED + imagePath);
            } catch (IOException e) {
                Debug.error(Lang.CHARACTER_IMAGE_UPDATE_ERROR + e.getMessage());
            }
        }
    }
    
    public boolean isMoving() {
        return false;
    }
    
    public Point getCurrentPosition() {
        return new Point(position);
    }
}
