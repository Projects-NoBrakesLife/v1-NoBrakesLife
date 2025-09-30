package ui;

import core.Config;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import util.BackgroundUtil;

public class MapCanvas extends JPanel {
    static class Obj {
        String file;
        String name;
        Image img;
        int x, y, w, h;
        double rotation = 0;

        @Override
        public String toString() {
            return name;
        }
    }

    private final Image background;
    private final List<Obj> objects = new ArrayList<>();
    private Obj selected;
    private Point dragAnchor;
    private double zoom = 1.0;

    public MapCanvas(String backgroundPath) {
        background = new ImageIcon(backgroundPath).getImage();
        setPreferredSize(new Dimension(Config.GAME_WIDTH, Config.GAME_HEIGHT));

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = toWorld(e.getPoint());
                for (int i = objects.size() - 1; i >= 0; i--) {
                    Obj o = objects.get(i);
                    if (new Rectangle(o.x, o.y, o.w, o.h).contains(p)) {
                        selected = o;
                        dragAnchor = new Point(p.x - o.x, p.y - o.y);
                        repaint();
                        return;
                    }
                }
                selected = null;
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (selected != null && dragAnchor != null) {
                    Point p = toWorld(e.getPoint());
                    selected.x = p.x - dragAnchor.x;
                    selected.y = p.y - dragAnchor.y;
                    repaint();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (selected != null) {
                    double f = e.getWheelRotation() < 0 ? 1.1 : 0.9;
                    scaleSelected(f);
                } else {
                    zoom = Math.max(0.25, Math.min(3.0, zoom * (e.getWheelRotation() < 0 ? 1.1 : 0.9)));
                }
                revalidate();
                repaint();
            }
        };

        addMouseListener(ma);
        addMouseMotionListener(ma);
        addMouseWheelListener(ma);

        setFocusable(true);
    }

    private Point toWorld(Point p) {
        return new Point((int) (p.x / zoom), (int) (p.y / zoom));
    }

    public Obj addPng(File f) throws Exception {
        BufferedImage img = ImageIO.read(f);
        Obj o = new Obj();
        o.file = f.getPath().replace("\\", "/");
        o.name = f.getName().replace(".png", "").replace("_", " ");

        o.img = img;
        o.w = img.getWidth();
        o.h = img.getHeight();
        o.x = Config.GAME_WIDTH / 2 - o.w / 2;
        o.y = Config.GAME_HEIGHT / 2 - o.h / 2;
        objects.add(o);
        repaint();
        return o;
    }

    public void addHappinessIcon() {
        try {
            File f = new File("assets/ui/emote/Icon Happiness #6686.png");
            addPng(f);
        } catch (Exception e) {
        }
    }

    public void addClockTower() {
        try {
            File f = new File("assets/ui/clock/Clock-Tower.png");
            addPng(f);
        } catch (Exception e) {
        }
    }

    public List<Obj> getObjects() {
        return objects;
    }

    public void removeObj(Obj o) {
        objects.remove(o);
        if (selected == o)
            selected = null;
        repaint();
    }

    public void duplicateSelected() {
        if (selected != null) {
            Obj copy = new Obj();
            copy.file = selected.file;
            copy.name = selected.name + "_copy";
            copy.img = selected.img;
            copy.x = selected.x + 20;
            copy.y = selected.y + 20;
            copy.w = selected.w;
            copy.h = selected.h;
            copy.rotation = selected.rotation;
            objects.add(copy);
            selected = copy;
            repaint();
        }
    }

    public void setSelected(Obj o) {
        selected = o;
        repaint();
    }

    public void rotateSelected(int deg) {
        if (selected != null) {
            selected.rotation += deg;
            repaint();
        }
    }

    public void scaleSelected(double factor) {
        if (selected != null) {
            selected.w = (int) Math.max(1, selected.w * factor);
            selected.h = (int) Math.max(1, selected.h * factor);
            repaint();
        }
    }

    public Obj openFileAndAdd() {
        JFileChooser fc = new JFileChooser(new File("./src"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG Images", "png"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                return addPng(fc.getSelectedFile());
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public String exportAsJavaList() {
        StringBuilder sb = new StringBuilder();
        sb.append("objects = java.util.List.of(\n");
        for (int i = 0; i < objects.size(); i++) {
            Obj o = objects.get(i);

            int originalX = o.x;
            int originalY = o.y;
            int originalW = o.w;
            int originalH = o.h;

            sb.append("    new GameObject(\"")
                    .append(o.file).append("\", \"")
                    .append(o.name).append("\", ")
                    .append(originalX).append(", ")
                    .append(originalY).append(", ")
                    .append(originalW).append(", ")
                    .append(originalH).append(", ")
                    .append((int) o.rotation).append(")");
            if (i < objects.size() - 1)
                sb.append(",");
            sb.append("\n");
        }
        sb.append(");\n");
        return sb.toString();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.scale(zoom, zoom);

        Rectangle d = BackgroundUtil.getBackgroundDest(background);
        g.drawImage(background, d.x, d.y, d.width, d.height, null);

        for (Obj o : objects) {
            int cx = o.x + o.w / 2;
            int cy = o.y + o.h / 2;
            g.rotate(Math.toRadians(o.rotation), cx, cy);
            g.drawImage(o.img, o.x, o.y, o.w, o.h, null);
            g.rotate(-Math.toRadians(o.rotation), cx, cy);

            if (o == selected) {
                g.setColor(Color.RED);
                g.drawRect(o.x, o.y, o.w, o.h);
            }
        }
    }
}
