package core;

import java.awt.*;
import javax.swing.*;

public class GameObject {
    public String file;
    public String name;
    public int x, y, w, h, rotation;
    public Image img;
    public boolean hovered = false;

    public GameObject(String file, String name, int x, int y, int w, int h, int rotation) {
        this.file = file;
        this.name = name;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.rotation = rotation;
        this.img = new ImageIcon(file).getImage();
    }

    public void draw(Graphics2D g2) {
        int drawW = hovered ? (int) (w * 1.05) : w;
        int drawH = hovered ? (int) (h * 1.05) : h;
        int drawX = x - (drawW - w) / 2;
        int drawY = y - (drawH - h) / 2;

        int cx = drawX + drawW / 2;
        int cy = drawY + drawH / 2;
        g2.rotate(Math.toRadians(rotation), cx, cy);
        g2.drawImage(img, drawX, drawY, drawW, drawH, null);
        g2.rotate(-Math.toRadians(rotation), cx, cy);
    }

    public boolean contains(Point p) {
        return new Rectangle(x, y, w, h).contains(p);
    }
}
