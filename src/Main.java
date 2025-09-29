import core.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import network.*;
import ui.WindowManager;
import ui.CharacterSelection;
import util.*;

public class Main extends JPanel {
    private final Image background;
    private final Image uiBox;
    private java.util.List<GameObject> objects;

    private GameObject hoveredObj = null;
    private final SoundPlayer soundPlayer = new SoundPlayer();
    private final WindowManager windowManager = new WindowManager();
    private boolean soundPlayed = false;
    private core.Character character;
    private PlayerState playerState;
    private boolean debugMode = false;
    private NetworkClient networkClient;
    private Map<String, core.Character> onlineCharacters = new HashMap<>();
    private String characterImage;

    private Point mouseOffset = new Point();
    private JFrame parentFrame;
    private java.util.List<Point> pathPoints = new java.util.ArrayList<>();
    private Point startPoint;

    private java.util.Map<PlayerState.Location, java.util.List<Point>> locationPaths = new java.util.HashMap<>();
    private boolean isMoving = false;
    private javax.swing.Timer moveTimer;
    private java.util.List<Point> currentPath;
    private int currentPathIndex = 0;
    private PlayerState.Location targetLocation;
    private GameObject targetObject;

    public Main() {
        background = new ImageIcon("./assets/maps/background-iceland.png").getImage();
        uiBox = new ImageIcon("./assets/ui/Header_box.png").getImage();
        setPreferredSize(new Dimension(Config.GAME_WIDTH, Config.GAME_HEIGHT));
        setFocusable(true);
        requestFocusInWindow();

        initializeObjects();

        int initialX = 878 + (int) (Math.random() * 40 - 20);
        int initialY = 280 + (int) (Math.random() * 40 - 20);
        startPoint = new Point(initialX, initialY);
        character = new core.Character(new Point(initialX, initialY));
        playerState = new PlayerState();
        System.out.println("Character created");
        playerState.printStatus();

        String playerId = "player" + System.currentTimeMillis();
        String characterImage = character.getImagePath();
        networkClient = new NetworkClient(playerId, "Player", new Point(initialX, initialY), characterImage);
        networkClient.connect();

        setupLocationPaths();

        javax.swing.Timer networkTimer = new javax.swing.Timer(50, e -> {
            updateOnlinePlayers();
        });
        networkTimer.start();

        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                    System.exit(0);
                } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_F1) {
                    debugMode = !debugMode;
                    if (debugMode) {
                        pathPoints.clear();
                        System.out.println("Debug mode ON - Click to add path points");
                    } else {
                        System.out.println("Debug mode OFF");
                    }
                    repaint();
                } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_F2 && debugMode) {
                    exportPathCode();
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                GameObject newHover = null;
                for (GameObject obj : objects) {
                    if (obj.name.equals("Clock-Tower"))
                        continue;
                    boolean inside = obj.contains(e.getPoint());
                    obj.hovered = inside;
                    if (inside)
                        newHover = obj;
                }

                if (newHover != hoveredObj) {
                    hoveredObj = newHover;
                    soundPlayed = false;
                }

                if (hoveredObj != null && !soundPlayed) {
                    soundPlayer.play("./assets/sfx/UI_Click_Organic_mono.wav");
                    soundPlayed = true;
                }

                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (debugMode) {
                    if (e.isControlDown()) {
                        startPoint = e.getPoint();
                        System.out.println("Start point set to: " + startPoint);
                    } else {
                        pathPoints.add(e.getPoint());
                        System.out.println("Added path point: " + e.getPoint() + " (Total: " + pathPoints.size() + ")");
                    }
                    repaint();
                    return;
                }

                if (isMoving) {
                    System.out.println("Character is moving, please wait...");
                    return;
                }

                for (GameObject obj : objects) {
                    if (obj.name.equals("Clock-Tower"))
                        continue;
                    if (obj.contains(e.getPoint())) {
                        showObjectWindow(obj);
                        break;
                    }
                }
            }
        });
    }

    private void initializeObjects() {
        objects = java.util.List.of(
                new GameObject("./assets/maps/obj/Apartment_Shitty-0.png",
                        "Apartment Shitty-0", 878, 58, 171, 213, 0),
                new GameObject("./assets/maps/obj/Bank-0.png", "Bank-0", 1344, 327, 241, 186, 90),
                new GameObject("./assets/ui/clock/Clock-Tower.png", "Clock-Tower", 735, 698, 447, 472, 0));
    }

    public void selectCharacter() {
        String selectedImage = CharacterSelection.showCharacterSelection(parentFrame);
        characterImage = selectedImage;
        character = new core.Character(character.getPosition(), selectedImage);
        System.out.println("Character selected: " + selectedImage);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        Rectangle d = BackgroundUtil.getBackgroundDest(background);
        g.drawImage(background, d.x, d.y, d.width, d.height, this);

        for (GameObject obj : objects) {
            if (obj.name.equals("Clock-Tower")) {
                obj.hovered = false;
            }
            obj.draw(g);
        }

        if (hoveredObj != null) {
            int boxW = (int) (uiBox.getWidth(null) * Config.UI_SCALE);
            int boxH = (int) (uiBox.getHeight(null) * Config.UI_SCALE);
            int cx = Config.GAME_WIDTH / 2 - boxW / 2;
            int cy = Config.GAME_HEIGHT / Config.UI_OFFSET_Y - boxH / 2;

            g.drawImage(uiBox, cx, cy, boxW, boxH, null);

            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            FontMetrics fm = g.getFontMetrics();
            int textW = fm.stringWidth(hoveredObj.name);
            int tx = Config.GAME_WIDTH / 2 - textW / 2;
            int ty = cy + (boxH / 2) + 20;

            g.drawString(hoveredObj.name, tx, ty);
        }

        if (character != null) {
            character.draw((Graphics2D) g);
        }

        for (core.Character onlineChar : onlineCharacters.values()) {
            onlineChar.draw((Graphics2D) g);
        }

        if (debugMode) {
            g.setColor(Color.GREEN);
            g.fillOval(startPoint.x - 8, startPoint.y - 8, 16, 16);
            g.setColor(Color.BLACK);
            g.drawString("START", startPoint.x - 15, startPoint.y - 15);

            g.setColor(Color.BLUE);
            g.setStroke(new java.awt.BasicStroke(3));
            for (int i = 0; i < pathPoints.size() - 1; i++) {
                Point p1 = pathPoints.get(i);
                Point p2 = pathPoints.get(i + 1);
                g.drawLine(p1.x, p1.y, p2.x, p2.y);
            }

            g.setColor(Color.RED);
            for (Point p : pathPoints) {
                g.fillOval(p.x - 5, p.y - 5, 10, 10);
            }

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString("DEBUG MODE - F1: Exit | F2: Export Code", 10, 30);
            g.drawString("Click: Add point | Ctrl+Click: Set start", 10, 50);
        }
    }

    private void showObjectWindow(GameObject obj) {
        if (obj.name.equals("Apartment Shitty-0")) {
            moveToLocation(PlayerState.Location.APARTMENT_SHITTY, obj);
        } else if (obj.name.equals("Bank-0")) {
            moveToLocation(PlayerState.Location.BANK, obj);
        }
    }

    private void setupLocationPaths() {
        locationPaths.put(PlayerState.Location.APARTMENT_SHITTY, java.util.List.of(
                new Point(878, 280)));

        locationPaths.put(PlayerState.Location.BANK, java.util.List.of(
                new Point(922, 289),
                new Point(1223, 291),
                new Point(1308, 555),
                new Point(1437, 562)));
    }

    private void moveToLocation(PlayerState.Location targetLocation, GameObject obj) {
        PlayerState.Location currentLocation = playerState.getCurrentLocation();

        if (currentLocation == targetLocation) {
            System.out.println("Already at " + targetLocation);
            showWindowForLocation(targetLocation);
            return;
        }

        if (isMoving) {
            System.out.println("Character is already moving");
            return;
        }

        System.out.println("Moving from " + currentLocation + " to " + targetLocation);

        java.util.List<Point> path = getPathBetweenLocations(currentLocation, targetLocation);
        if (path != null && !path.isEmpty()) {
            startMovement(path, targetLocation, obj);
        } else {
            playerState.setCurrentLocation(targetLocation);
            updateCharacterPosition(targetLocation);
            showWindowForLocation(targetLocation);
        }
    }

    private java.util.List<Point> getPathBetweenLocations(PlayerState.Location from, PlayerState.Location to) {
        if (from == PlayerState.Location.APARTMENT_SHITTY && to == PlayerState.Location.BANK) {
            return java.util.List.of(
                    new Point(922, 289),
                    new Point(1223, 291),
                    new Point(1308, 555),
                    new Point(1437, 562));
        } else if (from == PlayerState.Location.BANK && to == PlayerState.Location.APARTMENT_SHITTY) {
            return java.util.List.of(
                    new Point(1308, 555),
                    new Point(1223, 291),
                    new Point(922, 289),
                    new Point(878, 280));
        }
        return null;
    }

    private void startMovement(java.util.List<Point> path, PlayerState.Location targetLocation, GameObject obj) {
        this.currentPath = path;
        this.targetLocation = targetLocation;
        this.targetObject = obj;
        this.currentPathIndex = 0;
        this.isMoving = true;

        System.out.println("Starting movement along path with " + path.size() + " points");

        moveTimer = new javax.swing.Timer(50, e -> {
            if (currentPathIndex < currentPath.size()) {
                Point targetPoint = currentPath.get(currentPathIndex);
                character.setPosition(targetPoint);
                playerState.setCurrentPosition(targetPoint);
                networkClient.sendPlayerMove(targetPoint);
                currentPathIndex++;
                repaint();
            } else {
                finishMovement();
            }
        });
        moveTimer.start();
    }

    private void finishMovement() {
        moveTimer.stop();
        isMoving = false;
        playerState.setCurrentLocation(targetLocation);
        System.out.println("Arrived at " + targetLocation);
        playerState.printStatus();
        showWindowForLocation(targetLocation);
    }

    private void showWindowForLocation(PlayerState.Location location) {
        if (location == PlayerState.Location.APARTMENT_SHITTY) {
            windowManager.showWindow("apartment", "Crappy Apartment");
        } else if (location == PlayerState.Location.BANK) {
            windowManager.showWindow("bank", "Bank");
        }
    }

    private void updateCharacterPosition(PlayerState.Location location) {
        java.util.List<Point> path = locationPaths.get(location);
        if (path != null && !path.isEmpty()) {
            Point position = path.get(path.size() - 1);
            character.setPosition(position);
            playerState.setCurrentPosition(position);
        }
    }

    private void exportPathCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("Point startPoint = new Point(").append(startPoint.x).append(", ").append(startPoint.y)
                .append(");\n");
        sb.append("List<Point> pathPoints = List.of(\n");
        for (int i = 0; i < pathPoints.size(); i++) {
            Point p = pathPoints.get(i);
            sb.append("    new Point(").append(p.x).append(", ").append(p.y).append(")");
            if (i < pathPoints.size() - 1)
                sb.append(",");
            sb.append("\n");
        }
        sb.append(");\n");

        String code = sb.toString();
        System.out.println("=== PATH CODE ===");
        System.out.println(code);
        System.out.println("================");

        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new java.awt.datatransfer.StringSelection(code), null);
        System.out.println("Code copied to clipboard!");
    }

    private void updateOnlinePlayers() {
        Map<String, OnlinePlayer> players = networkClient.getOnlinePlayers();

        Set<String> currentPlayerIds = new HashSet<>(players.keySet());
        Set<String> characterIds = new HashSet<>(onlineCharacters.keySet());

        for (String playerId : currentPlayerIds) {
            OnlinePlayer player = players.get(playerId);
            if (!onlineCharacters.containsKey(playerId)) {
                onlineCharacters.put(playerId, new core.Character(player.position, player.characterImage));
                System.out.println("Created online character for: " + playerId + " with " + player.characterImage);
            } else {
                onlineCharacters.get(playerId).setPosition(player.position);
            }
        }

        for (String characterId : characterIds) {
            if (!currentPlayerIds.contains(characterId)) {
                onlineCharacters.remove(characterId);
                System.out.println("Removed online character for: " + characterId);
            }
        }

        repaint();
    }

    public void mousePressed(java.awt.event.MouseEvent e) {
        mouseOffset.x = e.getX();
        mouseOffset.y = e.getY();
    }

    public void mouseDragged(java.awt.event.MouseEvent e) {
        if (parentFrame != null) {
            Point newLocation = new Point(
                    parentFrame.getLocation().x + e.getX() - mouseOffset.x,
                    parentFrame.getLocation().y + e.getY() - mouseOffset.y);
            parentFrame.setLocation(newLocation);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setUndecorated(true);
        frame.setResizable(false);

        Main gamePanel = new Main();
        gamePanel.parentFrame = frame;
        frame.add(gamePanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        try {
            Image cursorImage = ImageIO.read(new File("./assets/ui/cone.png"));
            Cursor customCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, new Point(0, 0), "hand");
            frame.setCursor(customCursor);
        } catch (IOException e) {
        }
        gamePanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                gamePanel.mousePressed(e);
            }
        });

        gamePanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                gamePanel.mouseDragged(e);
            }
        });

        gamePanel.selectCharacter();
        frame.setVisible(true);

    }

}