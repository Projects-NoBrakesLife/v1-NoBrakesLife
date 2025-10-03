import core.GamePanel;
import core.Lang;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        JFrame frame = new JFrame(Lang.GAME_TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setUndecorated(true);
        frame.setResizable(false);

        GamePanel gamePanel = new GamePanel();
        gamePanel.setParentFrame(frame);
        frame.add(gamePanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        
        try {
            Image cursorImage = ImageIO.read(new File(Lang.CURSOR_IMAGE));
            Cursor customCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, new Point(0, 0), "hand");
            frame.setCursor(customCursor);
        } catch (IOException e) {
        }
        
        gamePanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                gamePanel.mousePressed(e);
            }
        });

        gamePanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                gamePanel.mouseDragged(e);
            }
        });

        frame.setVisible(true);
        gamePanel.selectCharacter();
    }
}