package ui;

import core.GameConfig;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import util.BackgroundUtil;

public class MapEditor extends JPanel {
    static class Obj {
        String file;
        String name;
        Image img;
        int x, y, w, h;
        double rotation = 0;
    }

    private final Image background;
    private final List<Obj> objects = new ArrayList<>();
    private final List<Point> roadPoints = new ArrayList<>();
    private Obj selected;
    private Point dragAnchor;
    private double zoom = 1.0;
    private boolean roadEditMode = false;
    private boolean showRoads = true;

    public MapEditor(String backgroundPath) {
        background = new ImageIcon(backgroundPath).getImage();
        setPreferredSize(new Dimension(GameConfig.Display.GAME_WIDTH, GameConfig.Display.GAME_HEIGHT));

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = toWorld(e.getPoint());
                
                if (roadEditMode) {
                    if (e.isControlDown()) {
                        roadPoints.clear();
                        System.out.println("Road points cleared");
                    } else {
                        roadPoints.add(p);
                        System.out.println("Added road point: " + p + " (Total: " + roadPoints.size() + ")");
                    }
                    repaint();
                    return;
                }
                
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
                if (roadEditMode) return;
                
                if (selected != null && dragAnchor != null) {
                    Point p = toWorld(e.getPoint());
                    selected.x = p.x - dragAnchor.x;
                    selected.y = p.y - dragAnchor.y;
                    repaint();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (roadEditMode) return;
                
                if (selected != null) {
                    double f = e.getWheelRotation() < 0 ? 1.1 : 0.9;
                    selected.w = (int) Math.max(1, selected.w * f);
                    selected.h = (int) Math.max(1, selected.h * f);
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
    }

    private Point toWorld(Point p) {
        return new Point((int) (p.x / zoom), (int) (p.y / zoom));
    }

    public void addPng(File f) throws Exception {
        BufferedImage img = ImageIO.read(f);
        Obj o = new Obj();
        o.file = f.getPath().replace("\\", "/");
        o.name = f.getName().replace(".png", "").replace("_", " ");

        o.img = img;
        o.w = img.getWidth();
        o.h = img.getHeight();
        o.x = GameConfig.Display.GAME_WIDTH / 2 - o.w / 2;
        o.y = GameConfig.Display.GAME_HEIGHT / 2 - o.h / 2;
        objects.add(o);
        repaint();
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

    public String exportRoadPoints() {
        StringBuilder sb = new StringBuilder();
        sb.append("public static final List<Point> ROAD_POINTS = List.of(\n");
        for (int i = 0; i < roadPoints.size(); i++) {
            Point p = roadPoints.get(i);
            sb.append("    new Point(").append(p.x).append(", ").append(p.y).append(")");
            if (i < roadPoints.size() - 1)
                sb.append(",");
            sb.append("\n");
        }
        sb.append(");\n");
        return sb.toString();
    }

    public void toggleRoadEditMode() {
        roadEditMode = !roadEditMode;
        System.out.println("Road Edit Mode: " + (roadEditMode ? "ON" : "OFF"));
        repaint();
    }

    public void toggleShowRoads() {
        showRoads = !showRoads;
        repaint();
    }

    public void loadRoadPoints(List<Point> points) {
        roadPoints.clear();
        roadPoints.addAll(points);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.scale(zoom, zoom);

        Rectangle d = BackgroundUtil.getBackgroundDest(background);
        g.drawImage(background, d.x, d.y, d.width, d.height, null);

        if (showRoads && !roadPoints.isEmpty()) {
            g.setColor(Color.GRAY);
            g.setStroke(new java.awt.BasicStroke(4));
            for (int i = 0; i < roadPoints.size() - 1; i++) {
                Point p1 = roadPoints.get(i);
                Point p2 = roadPoints.get(i + 1);
                g.drawLine(p1.x, p1.y, p2.x, p2.y);
            }

            g.setColor(Color.DARK_GRAY);
            for (int i = 0; i < roadPoints.size(); i++) {
                Point p = roadPoints.get(i);
                g.fillOval(p.x - 6, p.y - 6, 12, 12);
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, 10));
                g.drawString(String.valueOf(i), p.x - 3, p.y + 3);
                g.setColor(Color.DARK_GRAY);
            }
        }

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

        if (roadEditMode) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString("ROAD EDIT MODE", 10, 30);
            g.drawString("Click: Add road point", 10, 50);
            g.drawString("Ctrl+Click: Clear all", 10, 70);
            g.drawString("Points: " + roadPoints.size(), 10, 90);
        }
    }

    public void rotateSelectedObject(int degrees) {
        if (selected != null) {
            selected.rotation += degrees;
            repaint();
        }
    }

    public void zoomIn() {
        zoom *= 1.2;
        repaint();
    }

    public void zoomOut() {
        zoom /= 1.2;
        repaint();
    }

    public void resetZoom() {
        zoom = 1.0;
        repaint();
    }
}
