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
    private javax.swing.Timer timeCheckTimer;

    public GamePanel() {
        background = new ImageIcon(Lang.BACKGROUND_IMAGE).getImage();
        uiBox = new ImageIcon(Lang.UI_BOX_IMAGE).getImage();
        setPreferredSize(new Dimension(Config.GAME_WIDTH, Config.GAME_HEIGHT));
        setFocusable(true);
        requestFocusInWindow();

        gameStateManager = GameStateManager.getInstance();
        unifiedDataSync = UnifiedDataSync.getInstance();

        initializeObjects();
        setupLocationPaths();
        pathFinder = new PathFinder(roadPoints);

        character = null;
        playerState = new PlayerState();
        characterHUD = new ui.CharacterHUD(playerState, 80, 100);
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
                
                if (networkClient != null) {
                    networkClient.sendPlayerTimeUpdate(playerState.getRemainingTime());
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isMyTurn()) {
                    return;
                }
                
                if (e.isControlDown()) {
                    String pointCode = "new Point(" + e.getX() + ", " + e.getY() + ")";
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                            new java.awt.datatransfer.StringSelection(pointCode), null);
                    return;
                }

                if (isMoving) {
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
            character = new core.Character(Config.APARTMENT_POINT, selectedImage);
            playerState.setCharacterImagePath(selectedImage);
            
            if (characterHUD != null) {
                characterHUD.loadCharacterIconNow();
            }
            
            Debug.log("Character selected: " + selectedImage);
            Debug.log("Character created at: " + character.getPosition());
            
            connectToServer(selectedImage);
        }
    }
    
    private void connectToServer(String selectedImage) {
        String playerId = "player" + System.currentTimeMillis();
        networkClient = new NetworkClient(playerId, Lang.DEFAULT_PLAYER_NAME, Config.APARTMENT_POINT, selectedImage);
        networkClient.setTurnChangeCallback(this::onTurnChanged);
        
        networkClient.connect();
        
        javax.swing.Timer connectionTimer = new javax.swing.Timer(100, e -> {
            if (networkClient.isConnected()) {
                gameStateManager.addPlayer(playerId, Lang.DEFAULT_PLAYER_NAME, selectedImage);
                startNetworkTimers();
                ((javax.swing.Timer) e.getSource()).stop();
            }
        });
        connectionTimer.start();
    }
    
    private void startNetworkTimers() {
        javax.swing.Timer initialUpdateTimer = new javax.swing.Timer(100, e -> {
            networkClient.sendPlayerUpdate();
            ((javax.swing.Timer) e.getSource()).stop();
        });
        initialUpdateTimer.start();

        javax.swing.Timer networkTimer = new javax.swing.Timer(100, _ -> {
            updateOnlinePlayers();
            checkPlayerCount();
        });
        networkTimer.start();
        
        javax.swing.Timer unifiedSyncTimer = new javax.swing.Timer(200, _ -> {
            syncPlayerDataUnified();
        });
        unifiedSyncTimer.start();
        
        waitingTimer = new javax.swing.Timer(500, _ -> {
            checkPlayerCount();
        });
        waitingTimer.start();
        
        timeCheckTimer = new javax.swing.Timer(1000, _ -> {
            checkTimeExpired();
        });
        timeCheckTimer.start();
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
            g.setFont(FontManager.getSmartThaiFont(20, Font.BOLD));
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
        
        drawTurnPopup((Graphics2D) g);
        drawTimeDisplay((Graphics2D) g);

        if (waitingForPlayers) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, Config.GAME_WIDTH, Config.GAME_HEIGHT);
            
            g.setColor(Color.WHITE);
            g.setFont(FontManager.getSmartThaiFont(24, Font.BOLD));
            FontMetrics fm = g.getFontMetrics();
            
            int totalPlayers = 1 + onlineCharacters.size();
            String waitingText = "รอผู้เล่นอื่น... (" + totalPlayers + "/" + Config.MIN_PLAYERS_TO_START + ")";
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
    }

    private void showObjectWindow(GameObject obj) {
        if (!isMyTurn()) {
            String currentTurnPlayer = gameStateManager.getCurrentTurnPlayer();
            String currentPlayerName = getPlayerNameById(currentTurnPlayer);
            Debug.log("❌ ไม่ใช่รอบของคุณ! รอรอบของ: " + currentPlayerName);
            return;
        }
        
        if (!playerState.hasTimeLeft()) {
            Debug.log("⏰ เวลาหมดแล้ว! ไม่สามารถเดินได้");
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
            String currentTurnPlayer = gameStateManager.getCurrentTurnPlayer();
            String currentPlayerName = getPlayerNameById(currentTurnPlayer);
            Debug.log("❌ ไม่ใช่รอบของคุณ! รอรอบของ: " + currentPlayerName);
            return;
        }
        
        if (character == null) {
            Debug.log("❌ ตัวละครยังไม่ได้สร้าง!");
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
            String currentTurnPlayer = gameStateManager.getCurrentTurnPlayer();
            String currentPlayerName = getPlayerNameById(currentTurnPlayer);
            Debug.log("❌ ไม่ใช่รอบของคุณ! รอรอบของ: " + currentPlayerName);
            return;
        }
        
        if (character == null) {
            Debug.log("❌ ตัวละครยังไม่ได้สร้าง!");
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
                        networkClient.sendPlayerTimeUpdate(playerState.getRemainingTime());
                    }
                }
                
                if (networkClient != null && currentPathIndex % 2 == 0) {
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
        
        if (playerState.hasTimeLeft()) {
            playerState.useTime(Config.TIME_PER_MOVEMENT);
            playerState.printStatus();
            showWindowForLocation(targetLocation);
            
            if (networkClient != null) {
                networkClient.sendPlayerStatsUpdate(playerState.getMoney(), playerState.getHealth(), playerState.getEnergy());
                int currentTime = playerState.getRemainingTime();
                if (currentTime != lastSentTime) {
                    networkClient.sendPlayerTimeUpdate(currentTime);
                    lastSentTime = currentTime;
                    Debug.log("ส่งข้อมูลเวลาเมื่อเดินเสร็จ: " + currentTime + " ชั่วโมง");
                }
            }
        } else {
            Debug.log("⏰ เวลาหมดแล้ว! เทิร์นนี้จบแล้ว");
            playerState.resetTime();
            if (isTurnBasedMode && isMyTurn()) {
                Debug.log("🎯 เวลาหมด - ส่งเทิร์นเสร็จสิ้น");
                sendTurnCompleteToServer();
            }
        }
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
        if (character == null) {
            Debug.log("❌ ตัวละครยังไม่ได้สร้าง!");
            return;
        }
        
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
    
    
    private boolean isTurnBasedMode = true;
    private long turnPopupStartTime = 0;
    private static final long TURN_POPUP_DURATION = 2000; 
    
    private GameStateManager gameStateManager;
    private UnifiedDataSync unifiedDataSync;
    
    
    private boolean isMyTurn() {
        if (!isTurnBasedMode) return true;
        if (networkClient == null) return true;
        
        String myPlayerId = networkClient.getMyPlayerData().playerId;
        boolean isMyTurn = gameStateManager.isPlayerTurn(myPlayerId);
        
        if (isMyTurn) {
            Debug.log("✅ เป็นเทิร์นของคุณ: " + myPlayerId);
        } else {
            String currentTurnPlayer = gameStateManager.getCurrentTurnPlayer();
            Debug.log("⏳ ไม่ใช่เทิร์นของคุณ - เทิร์นปัจจุบัน: " + currentTurnPlayer);
        }
        
        return isMyTurn;
    }
    
    private void updateOnlinePlayers() {
        if (networkClient == null) return;
        Map<String, OnlinePlayer> players = networkClient.getOnlinePlayers();

        Set<String> currentPlayerIds = new HashSet<>(players.keySet());
        Set<String> characterIds = new HashSet<>(onlineCharacters.keySet());

        long currentTime = System.currentTimeMillis();

        for (String playerId : currentPlayerIds) {
            OnlinePlayer player = players.get(playerId);
            if (player == null) {
                continue;
            }
            
            if (!onlineCharacters.containsKey(playerId)) {
                Point playerPosition = player.getPosition() != null ? player.getPosition() : Config.APARTMENT_POINT;
                String characterImage = player.getCharacterImage() != null ? player.getCharacterImage() : Lang.MALE_01;
                
                onlineCharacters.put(playerId, new core.Character(playerPosition, characterImage));
                lastKnownPositions.put(playerId, playerPosition);
                lastUpdateTimes.put(playerId, currentTime);
                
                PlayerState playerState = new PlayerState();
                playerState.setPlayerName(player.getPlayerName());
                playerState.setCharacterImagePath(characterImage);
                playerState.setCurrentPosition(playerPosition);
                playerState.setMoney(player.getMoney());
                playerState.setHealth(player.getHealth());
                playerState.setEnergy(player.getEnergy());
                onlineHUDManager.addPlayer(playerId, playerState);
                
                gameStateManager.addPlayer(playerId, player.getPlayerName(), characterImage);
                
                java.util.List<String> allPlayerIds = new java.util.ArrayList<>();
                String myPlayerId = networkClient.getMyPlayerData().playerId;
                allPlayerIds.add(myPlayerId);
                allPlayerIds.addAll(networkClient.getOnlinePlayers().keySet());
                updatePlayerNumbers(allPlayerIds);
            } else {
                core.Character existingChar = onlineCharacters.get(playerId);
                if (existingChar != null) {
                    Point newPos = player.getPosition();
                    Point lastKnownPos = lastKnownPositions.get(playerId);
                    
                 
                    if (newPos != null && (lastKnownPos == null || !lastKnownPos.equals(newPos))) {
                        existingChar.setPosition(newPos);
                        lastKnownPositions.put(playerId, newPos);
                        lastUpdateTimes.put(playerId, currentTime);
                    }
            
                    String currentImage = existingChar.getImagePath();
                    String newImage = player.getCharacterImage();
                    if (newImage != null && !newImage.isEmpty() && !newImage.equals(currentImage)) {
                        existingChar.updateImage(newImage);
                        onlineHUDManager.updatePlayer(playerId, player);
                    }
              
                    gameStateManager.updatePlayerData(playerId, player.getMoney(), player.getHealth(), player.getEnergy(), player.getRemainingTime());
                    gameStateManager.updatePlayerPosition(playerId, newPos);
                    
           
                    if (currentTime % 1000 < 50) { 
                        Debug.log("อัปเดตข้อมูลผู้เล่น: " + playerId + " เวลา: " + player.getRemainingTime() + " ชั่วโมง");
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
                
     
                gameStateManager.removePlayer(characterId);
                
                java.util.List<String> allPlayerIds = new java.util.ArrayList<>();
                String myPlayerId = networkClient.getMyPlayerData().playerId;
                allPlayerIds.add(myPlayerId);
                allPlayerIds.addAll(networkClient.getOnlinePlayers().keySet());
                updatePlayerNumbers(allPlayerIds);
            }
        }

        repaint();
    }

    
    private void sendTurnCompleteToServer() {
        if (networkClient != null) {
            networkClient.sendTurnComplete();
            Debug.log("📤 ส่งข้อความเทิร์นเสร็จสิ้นไปยังเซิร์ฟเวอร์");
        }
    }
    
    private void updateTurnFromServer() {
        if (onlineHUDManager != null) {
            String currentTurn = gameStateManager.getCurrentTurnPlayer();
            onlineHUDManager.updateTurn(currentTurn);
        }
        if (characterHUD != null) {
            String myPlayerId = networkClient.getMyPlayerData().playerId;
            characterHUD.setCurrentTurn(gameStateManager.isPlayerTurn(myPlayerId));
        }
    }
    
    private void onTurnChanged(String newTurnPlayerId) {
        gameStateManager.nextTurn();
        
        turnPopupStartTime = System.currentTimeMillis();
        
        String playerName = getPlayerNameById(newTurnPlayerId);
        Debug.log("🎯 เทิร์นเปลี่ยนเป็น: " + playerName + " (" + newTurnPlayerId + ")");
        
        String myPlayerId = networkClient.getMyPlayerData().playerId;
        if (newTurnPlayerId.equals(myPlayerId)) {
            playerState.resetTime();
            Debug.log("✅ ตอนนี้เป็นเทิร์นของคุณแล้ว! เวลาถูกรีเซ็ตเป็น " + Config.TURN_TIME_HOURS + " ชั่วโมง");
        } else {
            Debug.log("⏳ รอเทิร์นของ: " + playerName + " - คุณไม่สามารถเดินได้");
        }
        
        updateTurnFromServer();
        repaint();
    }
    
    
    private void syncPlayerState() {
        if (characterHUD != null) {
            characterHUD.updatePlayerState(playerState);
        }
    }
    
    private int lastSentTime = -1;
    
    private void syncPlayerDataUnified() {
        if (networkClient != null && networkClient.isConnected() && character != null) {
            String playerId = "localPlayer";
            Point currentPos = character.getPosition();
            int money = playerState.getMoney();
            int health = playerState.getHealth();
            int energy = playerState.getEnergy();
            int remainingTime = playerState.getRemainingTime();
            
            if (unifiedDataSync.shouldSyncPlayerData(playerId, currentPos, money, health, energy, remainingTime)) {
                networkClient.sendPlayerMove(currentPos);
                networkClient.sendPlayerStatsUpdate(money, health, energy);
                networkClient.sendPlayerTimeUpdate(remainingTime);
                unifiedDataSync.updateLastSync(playerId, currentPos, money, health, energy, remainingTime);
            }
        }
    }
    
    private void updatePlayerNumbers(java.util.List<String> allPlayerIds) {
        String myPlayerId = networkClient.getMyPlayerData().playerId;
        
        java.util.Collections.sort(allPlayerIds);
        
        Debug.log("อัปเดต Player Numbers - รายการผู้เล่นทั้งหมด: " + allPlayerIds);
        
        for (int i = 0; i < allPlayerIds.size(); i++) {
            String playerId = allPlayerIds.get(i);
            int playerNumber = i + 1;
            
            if (playerId.equals(myPlayerId)) {
                if (characterHUD != null) {
                    characterHUD.setPlayerNumber(playerNumber);
                    Debug.log("ตั้งค่า Player Number สำหรับตัวเรา: P" + playerNumber + " (" + playerId + ")");
                }
            } else {
                if (onlineHUDManager != null) {
                    onlineHUDManager.setPlayerNumber(playerId, playerNumber);
                    Debug.log("ตั้งค่า Player Number สำหรับ " + playerId + ": P" + playerNumber);
                }
            }
        }
    }
    
    private void drawTurnPopup(Graphics2D g2d) {
        String currentTurnPlayer = gameStateManager.getCurrentTurnPlayer();
        if (currentTurnPlayer == null || networkClient == null) return;
        
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - turnPopupStartTime;
     
        if (elapsedTime > TURN_POPUP_DURATION) return;
  
        float alpha = 1.0f - (float) elapsedTime / TURN_POPUP_DURATION;
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        
        String myPlayerId = networkClient.getMyPlayerData().playerId;
        boolean isMyTurn = gameStateManager.isPlayerTurn(myPlayerId);
        
        int turnPlayerNumber = getPlayerNumber(currentTurnPlayer);
        
        int centerX = Config.GAME_WIDTH / 2;
        int centerY = Config.GAME_HEIGHT / 2;
        int tokenSize = 120; 
        
        g2d.setColor(new Color(0, 0, 0, (int)(180 * alpha)));
        g2d.fillRect(0, 0, Config.GAME_WIDTH, Config.GAME_HEIGHT);
        
        try {
            String tokenPath = "assets/ui/hud/Token P" + turnPlayerNumber + " Front.png";
            java.awt.image.BufferedImage tokenIcon = javax.imageio.ImageIO.read(new java.io.File(tokenPath));
            
            if (tokenIcon != null) {
                g2d.drawImage(tokenIcon, centerX - tokenSize/2, centerY - tokenSize/2, tokenSize, tokenSize, null);
            }
        } catch (Exception e) {
            g2d.setColor(new Color(isMyTurn ? 0 : 255, isMyTurn ? 255 : 255, 0, (int)(200 * alpha)));
            g2d.fillOval(centerX - tokenSize/2, centerY - tokenSize/2, tokenSize, tokenSize);
        }
        
        String playerName = getPlayerNameById(currentTurnPlayer);
        String turnText = isMyTurn ? "เทิร์นของคุณ!" : "เทิร์นของ " + playerName;
        
        g2d.setColor(new Color(255, 255, 255, (int)(255 * alpha)));
        g2d.setFont(FontManager.getSmartThaiFont(24, Font.BOLD));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(turnText);
        int textX = centerX - textWidth / 2;
        int textY = centerY + tokenSize/2 + 40;
        g2d.drawString(turnText, textX, textY);
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
    
    
    private void checkPlayerCount() {
        if (networkClient == null) return;
        
        int totalPlayers = 1 + onlineCharacters.size();
        boolean shouldWait = totalPlayers < Config.MIN_PLAYERS_TO_START;
        boolean gameReady = totalPlayers >= Config.MIN_PLAYERS_TO_START;
        
        if (waitingForPlayers != shouldWait) {
            waitingForPlayers = shouldWait;
            
            if (!waitingForPlayers && gameReady) {
                showInitialTurnPopup();
            }
        }
    }
    
    private void showInitialTurnPopup() {
        turnPopupStartTime = System.currentTimeMillis();
    }
    
    private void checkTimeExpired() {
        String currentTurnPlayer = gameStateManager.getCurrentTurnPlayer();
        if (currentTurnPlayer == null || !gameStateManager.isPlayerTurn(networkClient.getMyPlayerData().playerId)) return;
        
        if (!playerState.hasTimeLeft()) {
            Debug.log("⏰ เวลาหมดแล้ว! ส่งเทิร์นเสร็จสิ้นอัตโนมัติ");
            playerState.resetTime();
            sendTurnCompleteToServer();
        }
    }
    
    private void drawTimeDisplay(Graphics2D g2d) {
        if (playerState == null || waitingForPlayers) return;
        
        int currentPlayerTime = 0;
        String currentPlayerName = "";
        
        String currentTurnPlayer = gameStateManager.getCurrentTurnPlayer();
        if (currentTurnPlayer != null && networkClient != null) {
            String myPlayerId = networkClient.getMyPlayerData().playerId;
            if (currentTurnPlayer.equals(myPlayerId)) {
                currentPlayerTime = playerState.getRemainingTime();
                currentPlayerName = "คุณ (P" + getPlayerNumber(myPlayerId) + ")";
            } else {
                OnlinePlayer currentPlayer = networkClient.getOnlinePlayers().get(currentTurnPlayer);
                if (currentPlayer != null) {
                    currentPlayerTime = currentPlayer.getRemainingTime();
                    currentPlayerName = currentPlayer.getPlayerName() + " (P" + getPlayerNumber(currentTurnPlayer) + ")";
                } else {
                    currentPlayerTime = 24;
                    currentPlayerName = "ผู้เล่นอื่น (P" + getPlayerNumber(currentTurnPlayer) + ")";
                }
            }
        } else {
            currentPlayerTime = playerState.getRemainingTime();
            currentPlayerName = "คุณ (P1)";
        }
        
        String timeText = currentPlayerName + " - เวลาที่เหลือ: " + currentPlayerTime + " ชั่วโมง";
        
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(Config.GAME_WIDTH/2 - 200, Config.GAME_HEIGHT - 60, 400, 40, 10, 10);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(FontManager.getSmartThaiFont(Config.TIME_DISPLAY_FONT_SIZE, Font.BOLD));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(timeText);
        int textX = Config.GAME_WIDTH/2 - textWidth/2;
        int textY = Config.GAME_HEIGHT - 30;
        
        g2d.drawString(timeText, textX, textY);
    }
    
    private int getPlayerNumber(String playerId) {
        if (networkClient == null) return 1;
        
        java.util.List<String> allPlayerIds = new java.util.ArrayList<>();
        String myPlayerId = networkClient.getMyPlayerData().playerId;
        allPlayerIds.add(myPlayerId);
        allPlayerIds.addAll(networkClient.getOnlinePlayers().keySet());
        java.util.Collections.sort(allPlayerIds);
        
        int index = allPlayerIds.indexOf(playerId);
        return index >= 0 ? index + 1 : 1;
    }
}
