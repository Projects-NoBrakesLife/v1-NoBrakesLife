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
import util.FontManager;
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
        background = new ImageIcon(Lang.BACKGROUND_IMAGE).getImage();
        uiBox = new ImageIcon(Lang.UI_BOX_IMAGE).getImage();
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
        networkClient = new NetworkClient(playerId, Lang.DEFAULT_PLAYER_NAME, Config.APARTMENT_POINT, characterImagePath);
        networkClient.connect();

        javax.swing.Timer networkTimer = new javax.swing.Timer(Config.NETWORK_UPDATE_INTERVAL, e -> {
            updateOnlinePlayers();
            syncPlayerState();
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
                    if (obj.name.equals(Lang.CLOCK_TOWER_NAME))
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
                    soundPlayer.play(Lang.UI_CLICK_SOUND);
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
                    Debug.log(Lang.COPIED + pointCode);
                    return;
                }

                if (isMoving) {
                    Debug.log(Lang.CHARACTER_MOVING);
                    return;
                }

                for (GameObject obj : objects) {
                    if (obj.name.equals(Lang.CLOCK_TOWER_NAME))
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
                        Lang.APARTMENT_NAME,  727, 39, 152, 192, 0),
                new GameObject("./assets/maps/obj/Bank-0.png", "./assets/maps/obj/Bank-1.png", Lang.BANK_NAME, 1131, 291, 181, 132, 90),
                new GameObject("./assets/maps/obj/Tech-0.png", "./assets/maps/obj/Tech-1.png", Lang.TECH_NAME, 1259, 471, 143, 150, 0),
                new GameObject("./assets/maps/obj/Job_Office-0.png","./assets/maps/obj/Job_Office-1.png", Lang.JOB_OFFICE_NAME, 911, 466, 208, 176, 0),
                new GameObject("./assets/maps/obj/Culture-0.png","./assets/maps/obj/Culture-1.png", Lang.CULTURE_NAME, 711, 462, 186, 105, 0),
                new GameObject("./assets/maps/obj/University-0.png", "./assets/maps/obj/University-1.png", Lang.UNIVERSITY_NAME, 462, 489, 168, 129, 0),
              
                new GameObject("./assets/ui/clock/Clock-Tower.png", Lang.CLOCK_TOWER_NAME,633, 615, 336, 352, 0));
    }

    private void setupLocationPaths() {
        locationPoints.put(PlayerState.Location.APARTMENT_SHITTY, Config.APARTMENT_POINT);
        locationPoints.put(PlayerState.Location.BANK, Config.BANK_POINT);
        locationPoints.put(PlayerState.Location.TECH, Config.TECH_POINT);
        locationPoints.put(PlayerState.Location.JOB_OFFICE, Config.JOB_OFFICE_POINT);
        locationPoints.put(PlayerState.Location.CULTURE, Config.CULTURE_POINT);
        locationPoints.put(PlayerState.Location.UNIVERSITY, Config.UNIVERSITY_POINT);

        roadPoints.addAll(Config.ROAD_POINTS);
    }

    public void selectCharacter() {
        String selectedImage = CharacterSelection.showCharacterSelection(parentFrame);
        character = new core.Character(character.getPosition(), selectedImage);
        networkClient.sendPlayerUpdate();
        Debug.log(Lang.CHARACTER_SELECTED + selectedImage);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        Rectangle d = BackgroundUtil.getBackgroundDest(background);
        g.drawImage(background, d.x, d.y, d.width, d.height, this);

        for (GameObject obj : objects) {
            if (obj.name.equals(Lang.CLOCK_TOWER_NAME)) {
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
            g.setFont(FontManager.getFontForText(hoveredObj.name, 20, Font.BOLD));
            FontMetrics fm = g.getFontMetrics();
            int textW = fm.stringWidth(hoveredObj.name);
            int tx = Config.GAME_WIDTH / 2 - textW / 2;
            int ty = cy + (boxH / 2) + 30;

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
        g.setFont(FontManager.getFontForText(Lang.CLICK_BUILDINGS_HINT, 14, Font.BOLD));
        g.drawString(Lang.CLICK_BUILDINGS_HINT, 10, Config.GAME_HEIGHT - 50);
        g.setFont(FontManager.getFontForText(Lang.CURRENT_LOCATION + playerState.getCurrentLocation(), 14, Font.BOLD));
        g.drawString(Lang.CURRENT_LOCATION + playerState.getCurrentLocation(), 10, Config.GAME_HEIGHT - 30);
        g.setFont(FontManager.getFontForText(Lang.MOUSE_POSITION + mousePosition.x + ", " + mousePosition.y, 14, Font.BOLD));
        g.drawString(Lang.MOUSE_POSITION + mousePosition.x + ", " + mousePosition.y, 10, Config.GAME_HEIGHT - 10);
    }

    private void showObjectWindow(GameObject obj) {
        if (obj.name.equals(Lang.APARTMENT_NAME)) {
            moveToLocation(PlayerState.Location.APARTMENT_SHITTY, obj);
        } else if (obj.name.equals(Lang.BANK_NAME)) {
            moveToLocation(PlayerState.Location.BANK, obj);
        } else if (obj.name.equals(Lang.TECH_NAME)) {
            moveToLocation(PlayerState.Location.TECH, obj);
        } else if (obj.name.equals(Lang.JOB_OFFICE_NAME)) {
            moveToLocation(PlayerState.Location.JOB_OFFICE, obj);
        } else if (obj.name.equals(Lang.CULTURE_NAME)) {
            moveToLocation(PlayerState.Location.CULTURE, obj);
        } else if (obj.name.equals(Lang.UNIVERSITY_NAME)) {
            moveToLocation(PlayerState.Location.UNIVERSITY, obj);
        }
    }

    private void moveToLocation(PlayerState.Location targetLocation, GameObject obj) {
        PlayerState.Location currentLocation = playerState.getCurrentLocation();

        if (currentLocation == targetLocation) {
            Debug.log(Lang.ALREADY_AT_LOCATION + targetLocation);
            showWindowForLocation(targetLocation);
            return;
        }

        if (isMoving) {
            Debug.log(Lang.ALREADY_MOVING);
            return;
        }

        Debug.log(Lang.MOVING_FROM_TO + currentLocation + Lang.TO + targetLocation);

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

        Debug.log(Lang.STARTING_MOVEMENT + path.size() + Lang.POINTS);

        moveTimer = new javax.swing.Timer(Config.MOVEMENT_TIMER_INTERVAL, e -> {
            if (currentPathIndex < currentPath.size()) {
                Point targetPoint = currentPath.get(currentPathIndex);
                character.setPosition(targetPoint);
                playerState.setCurrentPosition(targetPoint);
                networkClient.sendPlayerMove(targetPoint);
                networkClient.sendPlayerLocationChange(playerState.getCurrentLocation());
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
        Debug.log(Lang.ARRIVED_AT + targetLocation);
        playerState.printStatus();
        showWindowForLocation(targetLocation);
    }

    private void showWindowForLocation(PlayerState.Location location) {
        if (location == PlayerState.Location.APARTMENT_SHITTY) {
            windowManager.showWindow(Lang.APARTMENT_WINDOW, Lang.APARTMENT_TITLE);
        } else if (location == PlayerState.Location.BANK) {
            windowManager.showWindow(Lang.BANK_WINDOW, Lang.BANK_TITLE);
        } else if (location == PlayerState.Location.TECH) {
            windowManager.showWindow(Lang.TECH_WINDOW, Lang.TECH_TITLE);
        } else if (location == PlayerState.Location.JOB_OFFICE) {
            windowManager.showWindow(Lang.JOB_WINDOW, Lang.JOB_TITLE);
        } else if (location == PlayerState.Location.CULTURE) {
            windowManager.showWindow(Lang.CULTURE_WINDOW, Lang.CULTURE_TITLE);
        } else if (location == PlayerState.Location.UNIVERSITY) {
            windowManager.showWindow(Lang.UNIVERSITY_WINDOW, Lang.UNIVERSITY_TITLE);
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
                onlineCharacters.put(playerId, new core.Character(player.getPosition(), player.getCharacterImage()));
                Debug.log(Lang.ONLINE_CHARACTER_CREATED + playerId + Lang.ONLINE_CHARACTER_WITH + player.getCharacterImage());
            } else {
                core.Character existingChar = onlineCharacters.get(playerId);
                existingChar.setPosition(player.getPosition());
                existingChar.updateImage(player.getCharacterImage());
            }
        }

        for (String characterId : characterIds) {
            if (!currentPlayerIds.contains(characterId)) {
                onlineCharacters.remove(characterId);
                Debug.log(Lang.ONLINE_CHARACTER_REMOVED + characterId);
            }
        }

        repaint();
    }

    private void syncPlayerState() {
        if (networkClient.isConnected()) {
            networkClient.sendPlayerStatsUpdate(
                playerState.getMoney(),
                playerState.getHealth(),
                playerState.getEnergy()
            );
        }
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
