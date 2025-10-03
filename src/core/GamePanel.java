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
    private ui.CharacterHUD characterHUD;
    private ui.OnlinePlayerHUDManager onlineHUDManager;
    private boolean waitingForPlayers = true;
    private javax.swing.Timer waitingTimer;

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
        String characterImagePath = character.getImagePath();
        playerState.setCharacterImagePath(characterImagePath);
        characterHUD = new ui.CharacterHUD(playerState, 80, 100, 1);
        onlineHUDManager = new ui.OnlinePlayerHUDManager(new Dimension(Config.GAME_WIDTH, Config.GAME_HEIGHT));
        networkClient = null;

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
                if (!isMyTurn()) {
                    Debug.log("ไม่ใช่รอบของคุณ! รอรอบ: " + getPlayerNameById(currentTurnPlayer));
                    return;
                }
                
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
        if (selectedImage != null && !selectedImage.isEmpty()) {
            character = new core.Character(character.getPosition(), selectedImage);
            playerState.setCharacterImagePath(selectedImage);
            
            String playerId = "player" + System.currentTimeMillis();
            networkClient = new NetworkClient(playerId, Lang.DEFAULT_PLAYER_NAME, Config.APARTMENT_POINT, selectedImage);
            networkClient.connect();
            
            javax.swing.Timer initialUpdateTimer = new javax.swing.Timer(1000, e -> {
                networkClient.sendPlayerUpdate();
                ((javax.swing.Timer) e.getSource()).stop();
            });
            initialUpdateTimer.start();

            javax.swing.Timer networkTimer = new javax.swing.Timer(Config.NETWORK_UPDATE_INTERVAL, e -> {
                updateOnlinePlayers();
                syncPlayerState();
                checkPlayerCount();
            });
            networkTimer.start();
            
            waitingTimer = new javax.swing.Timer(1000, e -> {
                checkPlayerCount();
            });
            waitingTimer.start();
            
            Debug.log(Lang.CHARACTER_SELECTED + selectedImage);
        }
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

        if (character != null && !waitingForPlayers) {
            character.draw((Graphics2D) g);
        }

        if (!waitingForPlayers) {
            for (core.Character onlineChar : onlineCharacters.values()) {
                onlineChar.draw((Graphics2D) g);
            }
        }

        if (characterHUD != null) {
            characterHUD.draw((Graphics2D) g);
        }
        
        if (onlineHUDManager != null) {
            onlineHUDManager.drawAll((Graphics2D) g);
        }
        

        if (waitingForPlayers) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, Config.GAME_WIDTH, Config.GAME_HEIGHT);
            
            g.setColor(Color.WHITE);
            g.setFont(FontManager.getFontForText("รอผู้เล่นอื่น...", 24, Font.BOLD));
            FontMetrics fm = g.getFontMetrics();
            String waitingText = "รอผู้เล่นอื่น... (" + (1 + onlineCharacters.size()) + "/4)";
            int textWidth = fm.stringWidth(waitingText);
            int textX = (Config.GAME_WIDTH - textWidth) / 2;
            int textY = Config.GAME_HEIGHT / 2;
            g.drawString(waitingText, textX, textY);
        }

        g.setColor(Color.GRAY);
        g.setStroke(new java.awt.BasicStroke(3));
        for (int i = 0; i < roadPoints.size() - 1; i++) {
            Point p1 = roadPoints.get(i);
            Point p2 = roadPoints.get(i + 1);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

       /*  g.setColor(Color.CYAN);
        g.setFont(FontManager.getFontForText(Lang.CLICK_BUILDINGS_HINT, 14, Font.BOLD));
        g.drawString(Lang.CLICK_BUILDINGS_HINT, 10, Config.GAME_HEIGHT - 50);
        g.setFont(FontManager.getFontForText(Lang.CURRENT_LOCATION + playerState.getCurrentLocation(), 14, Font.BOLD));
        g.drawString(Lang.CURRENT_LOCATION + playerState.getCurrentLocation(), 10, Config.GAME_HEIGHT - 30);
        g.setFont(FontManager.getFontForText(Lang.MOUSE_POSITION + mousePosition.x + ", " + mousePosition.y, 14, Font.BOLD));
        g.drawString(Lang.MOUSE_POSITION + mousePosition.x + ", " + mousePosition.y, 10, Config.GAME_HEIGHT - 10); */
    }

    private void showObjectWindow(GameObject obj) {
        if (!isMyTurn()) {
            Debug.log("ไม่ใช่รอบของคุณ! รอรอบ: " + getPlayerNameById(currentTurnPlayer));
            return;
        }
        
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
        if (!isMyTurn()) {
            Debug.log("ไม่ใช่รอบของคุณ! รอรอบ: " + getPlayerNameById(currentTurnPlayer));
            return;
        }
        
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
        if (!isMyTurn()) {
            Debug.log("Not your turn! Current turn: " + currentTurnPlayer);
            return;
        }
        
        this.currentPath = path;
        this.targetLocation = targetLocation;
        this.targetObject = obj;
        this.currentPathIndex = 0;
        this.isMoving = true;

        Debug.log(Lang.STARTING_MOVEMENT + path.size() + Lang.POINTS);

        moveTimer = new javax.swing.Timer(Config.MOVEMENT_TIMER_INTERVAL, e -> {
            if (currentPathIndex < currentPath.size()) {
                Point targetPoint = currentPath.get(currentPathIndex);
                Point currentPos = character.getPosition();
                
                if (!targetPoint.equals(currentPos)) {
                    character.setPosition(targetPoint);
                    playerState.setCurrentPosition(targetPoint);
                    
                    if (networkClient != null) {
                        networkClient.sendPlayerMove(targetPoint);
                    }
                }
                
                if (networkClient != null) {
                    networkClient.sendPlayerLocationChange(playerState.getCurrentLocation());
                }
                
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
        
        if (isTurnBasedMode) {
            nextTurn();
        }
    }

    private void showWindowForLocation(PlayerState.Location location) {
        if (!isMyTurn()) {
            Debug.log("ไม่ใช่รอบของคุณ! รอรอบ: " + getPlayerNameById(currentTurnPlayer));
            return;
        }
        
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
            Point currentPos = character.getPosition();
            if (!position.equals(currentPos)) {
                character.setPosition(position);
                playerState.setCurrentPosition(position);
            }
        }
    }

    private Map<String, Point> lastKnownPositions = new HashMap<>();
    private Map<String, Long> lastUpdateTimes = new HashMap<>();
    private long lastPositionUpdate = 0;
    private static final long POSITION_UPDATE_INTERVAL = 500;
    private static final long INDIVIDUAL_UPDATE_INTERVAL = 300;
    
    private String currentTurnPlayer = null;
    private boolean isTurnBasedMode = true;
    
    private network.OnlineDataManager dataManager = new network.OnlineDataManagerImpl();
    
    private void initializeTurnSystem() {
        if (networkClient != null) {
            java.util.List<String> allPlayerIds = new java.util.ArrayList<>();
            
            String myPlayerId = networkClient.getMyPlayerData().playerId;
            allPlayerIds.add(myPlayerId);
            
            for (String playerId : networkClient.getOnlinePlayers().keySet()) {
                if (!allPlayerIds.contains(playerId)) {
                    allPlayerIds.add(playerId);
                }
            }
            
            if (allPlayerIds.size() > 0) {
                currentTurnPlayer = allPlayerIds.get(0);
                Debug.log("Turn system initialized. First turn: " + currentTurnPlayer + " (my ID: " + myPlayerId + ")");
                
                if (onlineHUDManager != null) {
                    onlineHUDManager.updateTurn(currentTurnPlayer);
                }
                if (characterHUD != null) {
                    characterHUD.setCurrentTurn(currentTurnPlayer.equals(myPlayerId));
                }
            }
        }
    }
    
    private void nextTurn() {
        if (networkClient != null) {
            java.util.List<String> allPlayerIds = new java.util.ArrayList<>();
            
            String myPlayerId = networkClient.getMyPlayerData().playerId;
            allPlayerIds.add(myPlayerId);
            
            for (String playerId : networkClient.getOnlinePlayers().keySet()) {
                if (!allPlayerIds.contains(playerId)) {
                    allPlayerIds.add(playerId);
                }
            }
            
            if (allPlayerIds.size() > 1) {
                int currentIndex = allPlayerIds.indexOf(currentTurnPlayer);
                int nextIndex = (currentIndex + 1) % allPlayerIds.size();
                currentTurnPlayer = allPlayerIds.get(nextIndex);
                Debug.log("Next turn: " + currentTurnPlayer + " (my ID: " + myPlayerId + ")");
                
                if (onlineHUDManager != null) {
                    onlineHUDManager.updateTurn(currentTurnPlayer);
                }
                if (characterHUD != null) {
                    characterHUD.setCurrentTurn(currentTurnPlayer.equals(myPlayerId));
                }
            }
        }
    }
    
    private boolean isMyTurn() {
        if (!isTurnBasedMode) return true;
        if (networkClient == null) return true;
        if (currentTurnPlayer == null) return true;
        
        String myPlayerId = networkClient.getMyPlayerData().playerId;
        boolean isMyTurn = currentTurnPlayer.equals(myPlayerId);
        Debug.log("Checking turn: current=" + currentTurnPlayer + ", my=" + myPlayerId + ", isMyTurn=" + isMyTurn);
        return isMyTurn;
    }
    
    private void updateOnlinePlayers() {
        if (networkClient == null) return;
        Map<String, OnlinePlayer> players = networkClient.getOnlinePlayers();

        Set<String> currentPlayerIds = new HashSet<>(players.keySet());
        Set<String> characterIds = new HashSet<>(onlineCharacters.keySet());

        long currentTime = System.currentTimeMillis();
        boolean shouldUpdate = (currentTime - lastPositionUpdate) > POSITION_UPDATE_INTERVAL;

        if (shouldUpdate) {
            Debug.log("Updating online players. Current: " + currentPlayerIds.size() + ", Characters: " + characterIds.size());
        }

        for (String playerId : currentPlayerIds) {
            OnlinePlayer player = players.get(playerId);
            if (player == null) {
                continue;
            }
            
            if (!onlineCharacters.containsKey(playerId)) {
                Point startPosition = new Point(779, 250);
                onlineCharacters.put(playerId, new core.Character(startPosition, player.getCharacterImage()));
                lastKnownPositions.put(playerId, startPosition);
                lastUpdateTimes.put(playerId, currentTime);
                Debug.log("Created new character for " + playerId + " at start position " + startPosition + " with image: " + player.getCharacterImage());
                
                if (player.getCharacterImage() != null && !player.getCharacterImage().isEmpty()) {
                    PlayerState playerState = new PlayerState();
                    playerState.setPlayerName(player.getPlayerName());
                    playerState.setCharacterImagePath(player.getCharacterImage());
                    playerState.setCurrentPosition(startPosition);
                    playerState.setMoney(player.getMoney());
                    playerState.setHealth(player.getHealth());
                    playerState.setEnergy(player.getEnergy());
                    onlineHUDManager.addPlayer(playerId, playerState);
                    Debug.log("Added HUD for player: " + playerId);
                }
            } else {
                core.Character existingChar = onlineCharacters.get(playerId);
                if (existingChar != null) {
                    Point currentPos = existingChar.getPosition();
                    Point newPos = player.getPosition();
                    Point lastKnownPos = lastKnownPositions.get(playerId);
                    Long lastUpdateTime = lastUpdateTimes.get(playerId);
                    
                    boolean shouldUpdateThisPlayer = (lastUpdateTime == null || (currentTime - lastUpdateTime) > INDIVIDUAL_UPDATE_INTERVAL);
                    boolean positionChanged = !newPos.equals(currentPos);
                    
                    if (shouldUpdateThisPlayer && positionChanged) {
                        existingChar.setPosition(newPos);
                        lastKnownPositions.put(playerId, newPos);
                        lastUpdateTimes.put(playerId, currentTime);
                        lastPositionUpdate = currentTime;
                        Debug.log("Updated position for " + playerId + " from " + currentPos + " to " + newPos + " (from server)");
                    }
                    
                    if (player.getCharacterImage() != null && !player.getCharacterImage().isEmpty()) {
                        existingChar.updateImage(player.getCharacterImage());
                        onlineHUDManager.updatePlayer(playerId, player);
                    }
                }
            }
        }

        for (String characterId : characterIds) {
            if (!currentPlayerIds.contains(characterId)) {
                onlineCharacters.remove(characterId);
                lastKnownPositions.remove(characterId);
                lastUpdateTimes.remove(characterId);
                onlineHUDManager.removePlayer(characterId);
                Debug.log("Removed character: " + characterId);
            }
        }

        if (shouldUpdate) {
            Debug.log("Final character count: " + onlineCharacters.size());
            repaint();
        }
        
        if (currentTurnPlayer == null && onlineCharacters.size() > 0) {
            initializeTurnSystem();
        }
    }

    
    private void syncPlayerState() {
        if (networkClient != null && networkClient.isConnected()) {
            if (dataManager.shouldUpdateStats("localPlayer", playerState.getMoney(), playerState.getHealth(), playerState.getEnergy())) {
                networkClient.sendPlayerStatsUpdate(playerState.getMoney(), playerState.getHealth(), playerState.getEnergy());
                dataManager.updatePlayerStats("localPlayer", playerState.getMoney(), playerState.getHealth(), playerState.getEnergy());
                Debug.log("Stats updated: Money=" + playerState.getMoney() + ", Health=" + playerState.getHealth() + ", Energy=" + playerState.getEnergy());
            }
        }
        if (characterHUD != null) {
            characterHUD.updatePlayerState(playerState);
        }
    }
    
    private String getPlayerNameById(String playerId) {
        if (networkClient != null) {
            Map<String, OnlinePlayer> players = networkClient.getOnlinePlayers();
            OnlinePlayer player = players.get(playerId);
            if (player != null) {
                return player.getPlayerName();
            }
        }
        return "ผู้เล่น";
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
    
    private Point findSafePosition(Point originalPosition) {
        Point safePosition = new Point(originalPosition);
        int minDistance = 200;
        int maxAttempts = 1;
        int attempt = 0;
        
        Debug.log("Finding safe position for: " + originalPosition);
        
        while (attempt < maxAttempts) {
            boolean collision = false;
            
            for (core.Character existingChar : onlineCharacters.values()) {
                Point existingPos = existingChar.getPosition();
                double distance = safePosition.distance(existingPos);
                
                if (distance < minDistance) {
                    collision = true;
                    double angle = Math.random() * 2 * Math.PI;
                    int offsetX = (int)(minDistance * Math.cos(angle));
                    int offsetY = (int)(minDistance * Math.sin(angle));
                    safePosition.x = existingPos.x + offsetX;
                    safePosition.y = existingPos.y + offsetY;
                    Debug.log("Collision detected, adjusting position to: " + safePosition);
                    break;
                }
            }
            
            if (character != null) {
                Point charPos = character.getPosition();
                double distance = safePosition.distance(charPos);
                
                if (distance < minDistance) {
                    collision = true;
                    double angle = Math.random() * 2 * Math.PI;
                    int offsetX = (int)(minDistance * Math.cos(angle));
                    int offsetY = (int)(minDistance * Math.sin(angle));
                    safePosition.x = charPos.x + offsetX;
                    safePosition.y = charPos.y + offsetY;
                    Debug.log("Collision with main character, adjusting position to: " + safePosition);
                }
            }
            
            if (!collision) break;
            
            attempt++;
        }
        
        if (safePosition.x < 300) safePosition.x = 300;
        if (safePosition.y < 300) safePosition.y = 300;
        if (safePosition.x > Config.GAME_WIDTH - 300) safePosition.x = Config.GAME_WIDTH - 300;
        if (safePosition.y > Config.GAME_HEIGHT - 300) safePosition.y = Config.GAME_HEIGHT - 300;
        
        Debug.log("Final safe position: " + safePosition);
        return safePosition;
    }
    
    private void checkPlayerCount() {
        if (networkClient == null) return;
        
        int totalPlayers = 1 + onlineCharacters.size();
        
        if (totalPlayers >= Config.MIN_PLAYERS_TO_START && waitingForPlayers) {
            waitingForPlayers = false;
            if (waitingTimer != null) {
                waitingTimer.stop();
            }
            Debug.log("Game started! All " + Config.MIN_PLAYERS_TO_START + " players are ready.");
        } else if (totalPlayers < Config.MIN_PLAYERS_TO_START && !waitingForPlayers) {
            waitingForPlayers = true;
            Debug.log("Waiting for more players... (" + totalPlayers + "/" + Config.MIN_PLAYERS_TO_START + ")");
        }
    }
}
