package ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;

public class HitEffect extends JPanel {
    private static final long serialVersionUID = 1L;
    private BufferedImage iconImage;
    private float y = 0;
    private float alpha = 1.0f;
    private Timer animationTimer;
    private boolean isActive = false;
    private static int activeCount = 0;
    private static long lastClickTime = 0;
    private static final long CLICK_COOLDOWN = 300;
    
    public HitEffect(int x, int y) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < CLICK_COOLDOWN) {
            return;
        }
        
        if (activeCount >= 2) return;
        
        setBounds(x - 15, y - 15, 30, 30);
        setOpaque(false);
        activeCount++;
        lastClickTime = currentTime;
        
        try {
            iconImage = ImageIO.read(new File("assets/ui/emote/Icon Happiness #6686.png"));
        } catch (IOException e) {
        }
        
        startAnimation();
    }
    
    private void startAnimation() {
        isActive = true;
        animationTimer = new Timer(16, e -> {
            y -= 4.0f;
            alpha -= 0.08f;
            
            if (alpha <= 0) {
                isActive = false;
                animationTimer.stop();
                activeCount--;
                SwingUtilities.invokeLater(() -> {
                    Container parent = getParent();
                    if (parent != null) {
                        parent.remove(this);
                        parent.repaint();
                    }
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    setLocation(getX(), (int)(getY() + y));
                    repaint();
                });
            }
        });
        animationTimer.start();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (iconImage != null && isActive) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.drawImage(iconImage, 0, 0, 30, 30, null);
        }
    }
    
    public boolean isActive() {
        return isActive;
    }
}
