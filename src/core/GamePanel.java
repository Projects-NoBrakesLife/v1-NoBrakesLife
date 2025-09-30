package core;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import network.NetworkClient;
import network.OnlinePlayer;
import ui.CharacterSelection;
import ui.WindowManager;
import util.BackgroundUtil;
import util.SoundPlayer;

public class GamePanel extends JPanel {
    private final Image background;
    private final Image uiBox;
    private java.util.List<GameObject> objects;
    private GameObject hoveredObj = null;
    private final SoundPlayer soundPlayer = new SoundPlayer();
    private final WindowManager windowManager = new WindowManager();
    private boolean soundPlayed = false;
    private core.Character character;
    private PlayerState playerState;
    private NetworkClient networkClient;
    private Map<String, core.Character> onlineCharacters = new HashMap<>();
    private Point mouseOffset = new Point();
    private JFrame parentFrame;
    private java.util.List<Point> roadPoints = new java.util.ArrayList<>();
    private java.util.Map<PlayerState.Location, Point> locationPoints = new java.util.HashMap<>();
    private Point mousePosition = new Point();
    private boolean isMoving = false;
    private javax.swing.Timer moveTimer;
    private java.util.List<Point> currentPath;
    private int currentPathIndex = 0;
    private PlayerState.Location targetLocation;
    private GameObject targetObject;
    private PathFinder pathFinder;

    public GamePanel() {
        background = new ImageIcon("./assets/maps/background-iceland.png").getImage();
        uiBox = new ImageIcon("./assets/ui/Header_box.png").getImage();
        setPreferredSize(new Dimension(Config.GAME_WIDTH, Config.GAME_HEIGHT));
        setFocusable(true);
        requestFocusInWindow();

        initializeObjects();
        setupLocationPaths();
        pathFinder = new PathFinder(roadPoints);

        character = new core.Character(Config.APARTMENT_POINT);
        playerState = new PlayerState();

        String playerId = "player" + System.currentTimeMillis();
        String characterImagePath = character.getImagePath();
        networkClient = new NetworkClient(playerId, "Player", Config.APARTMENT_POINT, characterImagePath);
        networkClient.connect();

        javax.swing.Timer networkTimer = new javax.swing.Timer(50, e -> {
            updateOnlinePlayers();
        });
        networkTimer.start();

        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                    System.exit(0);
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mousePosition = e.getPoint();
                
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
                if (e.isControlDown()) {
                    String pointCode = "new Point(" + e.getX() + ", " + e.getY() + ")";
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                            new java.awt.datatransfer.StringSelection(pointCode), null);
                    System.out.println("Copied: " + pointCode);
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
                new GameObject("./assets/maps/obj/Apartment_Shitty-0.png", "./assets/maps/obj/Apartment_Shitty-1.png",
                        "Apartment Shitty-0",  727, 39, 152, 192, 0),
                new GameObject("./assets/maps/obj/Bank-0.png", "./assets/maps/obj/Bank-1.png", "Bank-0", 1131, 291, 181, 132, 90),
                new GameObject("./assets/maps/obj/Tech-0.png", "./assets/maps/obj/Tech-1.png","Tech-0", 1259, 471, 143, 150, 0),
                new GameObject("./assets/maps/obj/Job_Office-0.png","./assets/maps/obj/Job_Office-1.png", "Job Office-0", 911, 466, 208, 176, 0),
                new GameObject("./assets/ui/clock/Clock-Tower.png", "Clock-Tower",633, 615, 336, 352, 0));
    }

    private void setupLocationPaths() {
        locationPoints.put(PlayerState.Location.APARTMENT_SHITTY, Config.APARTMENT_POINT);
        locationPoints.put(PlayerState.Location.BANK, Config.BANK_POINT);
        locationPoints.put(PlayerState.Location.TECH, Config.TECH_POINT);
        locationPoints.put(PlayerState.Location.JOB_OFFICE, Config.JOB_OFFICE_POINT);

        roadPoints.addAll(Config.ROAD_POINTS);
    }

    public void selectCharacter() {
        String selectedImage = CharacterSelection.showCharacterSelection(parentFrame);
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

        g.setColor(Color.GRAY);
        g.setStroke(new java.awt.BasicStroke(3));
        for (int i = 0; i < roadPoints.size() - 1; i++) {
            Point p1 = roadPoints.get(i);
            Point p2 = roadPoints.get(i + 1);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        g.setColor(Color.CYAN);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("Click on buildings to move automatically!", 10, Config.GAME_HEIGHT - 50);
        g.drawString("Current: " + playerState.getCurrentLocation(), 10, Config.GAME_HEIGHT - 30);
        g.drawString("Mouse: " + mousePosition.x + ", " + mousePosition.y, 10, Config.GAME_HEIGHT - 10);
    }

    private void showObjectWindow(GameObject obj) {
        if (obj.name.equals("Apartment Shitty-0")) {
            moveToLocation(PlayerState.Location.APARTMENT_SHITTY, obj);
        } else if (obj.name.equals("Bank-0")) {
            moveToLocation(PlayerState.Location.BANK, obj);
        } else if (obj.name.equals("Tech-0")) {
            moveToLocation(PlayerState.Location.TECH, obj);
        } else if (obj.name.equals("Job Office-0")) {
            moveToLocation(PlayerState.Location.JOB_OFFICE, obj);
        }
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

        Point fromPoint = locationPoints.get(currentLocation);
        Point toPoint = locationPoints.get(targetLocation);
        
        if (fromPoint != null && toPoint != null) {
            java.util.List<Point> path = pathFinder.findPath(fromPoint, toPoint);
            if (path != null && !path.isEmpty()) {
                startMovement(path, targetLocation, obj);
            } else {
                playerState.setCurrentLocation(targetLocation);
                updateCharacterPosition(targetLocation);
                showWindowForLocation(targetLocation);
            }
        }
    }

    private void startMovement(java.util.List<Point> path, PlayerState.Location targetLocation, GameObject obj) {
        this.currentPath = path;
        this.targetLocation = targetLocation;
        this.targetObject = obj;
        this.currentPathIndex = 0;
        this.isMoving = true;

        System.out.println("Starting movement along path with " + path.size() + " points");

        moveTimer = new javax.swing.Timer(16, e -> {
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
        } else if (location == PlayerState.Location.TECH) {
            windowManager.showWindow("tech", "Tech Store");
        } else if (location == PlayerState.Location.JOB_OFFICE) {
            windowManager.showWindow("job", "Job Office");
        }
    }

    private void updateCharacterPosition(PlayerState.Location location) {
        Point position = locationPoints.get(location);
        if (position != null) {
            character.setPosition(position);
            playerState.setCurrentPosition(position);
        }
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

    public void setParentFrame(JFrame frame) {
        this.parentFrame = frame;
    }
}
