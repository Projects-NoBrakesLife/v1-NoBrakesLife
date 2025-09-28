import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

public class PathEditor extends JPanel {
    private List<Point> pathPoints = new ArrayList<>();
    private Point currentPoint = null;
    private boolean isDragging = false;
    private BufferedImage background;
    private Point startPoint = new Point(947, 290);
    
    private List<GameObject> gameObjects = new ArrayList<>();
    
    public PathEditor() {
        setPreferredSize(new Dimension(1920, 1080));
        
        try {
            background = ImageIO.read(new File("assets/maps/background-iceland.png"));
        } catch (IOException e) {
        }
        
        addGameObjects();
        
        pathPoints.add(new Point(949, 291));
        pathPoints.add(new Point(1225, 288));
        pathPoints.add(new Point(1308, 561));
        pathPoints.add(new Point(1442, 565));
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (e.isControlDown()) {
                        startPoint = e.getPoint();
                    } else {
                        pathPoints.add(e.getPoint());
                    }
                    repaint();
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    pathPoints.clear();
                    repaint();
                }
            }
        });
        
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                currentPoint = e.getPoint();
                repaint();
            }
        });
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    pathPoints.clear();
                    repaint();
                }
            }
        });
        setFocusable(true);
    }
    
    private void addGameObjects() {
        try {
            BufferedImage apartment = ImageIO.read(new File("assets/maps/obj/Apartment_Shitty-0.png"));
            BufferedImage bank = ImageIO.read(new File("assets/maps/obj/Bank-0.png"));
            BufferedImage clock = ImageIO.read(new File("assets/ui/clock/Clock-Tower.png"));
            
            gameObjects.add(new GameObject("Apartment", apartment, 878, 58, 171, 213));
            gameObjects.add(new GameObject("Bank", bank, 1344, 327, 241, 186));
            gameObjects.add(new GameObject("Clock", clock, 735, 698, 447, 472));
        } catch (IOException e) {
        }
    }
    
    static class GameObject {
        String name;
        BufferedImage image;
        int x, y, w, h;
        
        GameObject(String name, BufferedImage image, int x, int y, int w, int h) {
            this.name = name;
            this.image = image;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (background != null) {
            Rectangle d = BackgroundUtil.getBackgroundDest(background);
            g2d.drawImage(background, d.x, d.y, d.width, d.height, null);
        }
        
        for (GameObject obj : gameObjects) {
            if (obj.image != null) {
                g2d.drawImage(obj.image, obj.x, obj.y, obj.w, obj.h, null);
            }
        }
        
        g2d.setColor(Color.GREEN);
        g2d.fillOval(startPoint.x - 8, startPoint.y - 8, 16, 16);
        g2d.setColor(Color.BLACK);
        g2d.drawString("START", startPoint.x - 15, startPoint.y - 15);
        
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(3));
        
        for (int i = 0; i < pathPoints.size() - 1; i++) {
            Point p1 = pathPoints.get(i);
            Point p2 = pathPoints.get(i + 1);
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
        
        g2d.setColor(Color.RED);
        for (Point p : pathPoints) {
            g2d.fillOval(p.x - 5, p.y - 5, 10, 10);
        }
        
        if (currentPoint != null) {
            g2d.setColor(Color.GRAY);
            g2d.fillOval(currentPoint.x - 3, currentPoint.y - 3, 6, 6);
        }
    }
    
    public String exportPath() {
        StringBuilder sb = new StringBuilder();
        sb.append("Point startPoint = new Point(").append(startPoint.x).append(", ").append(startPoint.y).append(");\n");
        sb.append("List<Point> pathPoints = List.of(\n");
        for (int i = 0; i < pathPoints.size(); i++) {
            Point p = pathPoints.get(i);
            sb.append("    new Point(").append(p.x).append(", ").append(p.y).append(")");
            if (i < pathPoints.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(");\n");
        return sb.toString();
    }
    
    public static void main(String[] args) {
        JFrame frame = new JFrame("Path Editor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        PathEditor editor = new PathEditor();
        frame.add(editor);
        
        JButton exportBtn = new JButton("Export Path");
        exportBtn.addActionListener(e -> {
            String code = editor.exportPath();
            JTextArea area = new JTextArea(code, 10, 50);
            area.setFont(new Font("Monospaced", Font.PLAIN, 12));
            JOptionPane.showMessageDialog(frame, new JScrollPane(area), "Path Code", JOptionPane.PLAIN_MESSAGE);
        });
        
        JLabel instructionLabel = new JLabel("Left click: Add path point | Ctrl+Left click: Set start point | Right click: Clear path | ESC: Clear all");
        instructionLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(instructionLabel, BorderLayout.CENTER);
        bottomPanel.add(exportBtn, BorderLayout.EAST);
        
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.pack();
        frame.setVisible(true);
    }
}