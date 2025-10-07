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
import network.PlayerData;
import ui.CharacterSelection;
import ui.WindowManager;
import util.BackgroundUtil;
import util.FontManager;
import util.SoundPlayer;

public class GamePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final Image background;
    private final Image uiBox;
    private java.util.List<GameObject> objects;
    private GameObject hoveredObj = null;
    private GameObjectFactory objectFactory;
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
    private javax.swing.Timer networkTimer;
    private long gameEndTime = 0;
    private String gameEndReason = "";

    public GamePanel() {
        background = new ImageIcon(Lang.BACKGROUND_IMAGE).getImage();
        uiBox = new ImageIcon(Lang.UI_BOX_IMAGE).getImage();
        setPreferredSize(new Dimension(GameConfig.Display.GAME_WIDTH, GameConfig.Display.GAME_HEIGHT));
        setFocusable(true);
        requestFocusInWindow();

        gameStateManager = GameStateManager.getInstance();
        coreDataManager = CoreDataManager.getInstance();
        coreDataManager.setGameEndCallback(this::onGameEnded);

        objectFactory = GameObjectFactory.getInstance();
        objects = objectFactory.getAllObjects();
        setupLocationPaths();
        pathFinder = new PathFinder(roadPoints);

        character = null;
        playerState = new PlayerState();
        characterHUD = new ui.CharacterHUD(playerState, 80, 100);
        onlineHUDManager = new ui.OnlinePlayerHUDManager(new Dimension(GameConfig.Display.GAME_WIDTH, GameConfig.Display.GAME_HEIGHT));
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


    private void setupLocationPaths() {
        locationPoints.put(PlayerState.Location.APARTMENT_SHITTY, GameConfig.Map.APARTMENT_POINT);
        locationPoints.put(PlayerState.Location.BANK, GameConfig.Map.BANK_POINT);
        locationPoints.put(PlayerState.Location.TECH, GameConfig.Map.TECH_POINT);
        locationPoints.put(PlayerState.Location.JOB_OFFICE, GameConfig.Map.JOB_OFFICE_POINT);
        locationPoints.put(PlayerState.Location.CULTURE, GameConfig.Map.CULTURE_POINT);
        locationPoints.put(PlayerState.Location.UNIVERSITY, GameConfig.Map.UNIVERSITY_POINT);

        roadPoints.addAll(GameConfig.Map.ROAD_POINTS);
    }

    public void selectCharacter() {
        String selectedImage = CharacterSelection.showCharacterSelection(parentFrame);
        if (selectedImage != null && !selectedImage.isEmpty()) {
            character = new core.Character(GameConfig.Map.APARTMENT_POINT, selectedImage);
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
        networkClient = new NetworkClient(playerId, Lang.DEFAULT_PLAYER_NAME, GameConfig.Map.APARTMENT_POINT, selectedImage);
        networkClient.setTurnChangeCallback(this::onTurnChanged);

        coreDataManager.addPlayer(playerId, Lang.DEFAULT_PLAYER_NAME, GameConfig.Map.APARTMENT_POINT, selectedImage);
        Debug.logTurn("Added myself to CoreDataManager: " + playerId + " (" + Lang.DEFAULT_PLAYER_NAME + ")");
        
        networkClient.connect();
        
        new javax.swing.Timer(100, new java.awt.event.ActionListener() {
            private int attempts = 0;
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (networkClient.isConnected()) {
                    gameStateManager.addPlayer(playerId, Lang.DEFAULT_PLAYER_NAME, selectedImage);
                    startNetworkTimers();
                    ((javax.swing.Timer) e.getSource()).stop();
                } else if (++attempts > 50) {
                    ((javax.swing.Timer) e.getSource()).stop();
                    Debug.log("Connection timeout");
                }
            }
        }).start();
    }
    
    private void startNetworkTimers() {
        new javax.swing.Timer(100, e -> {
            networkClient.sendPlayerUpdate();
            ((javax.swing.Timer) e.getSource()).stop();
        }).start();

        networkTimer = new javax.swing.Timer(100, _ -> {
            updateOnlinePlayers();
            checkPlayerCount();
        });
        networkTimer.start();

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
            int boxW = (int) (uiBox.getWidth(null) * GameConfig.Display.UI_SCALE);
            int boxH = (int) (uiBox.getHeight(null) * GameConfig.Display.UI_SCALE);
            int cx = GameConfig.Display.GAME_WIDTH / 2 - boxW / 2;
            int cy = GameConfig.Display.GAME_HEIGHT / GameConfig.Display.UI_OFFSET_Y - boxH / 2;

            g.drawImage(uiBox, cx, cy, boxW, boxH, null);

            g.setColor(Color.BLACK);
            g.setFont(FontManager.getSmartThaiFont(20, Font.BOLD));
            FontMetrics fm = g.getFontMetrics();
            int textW = fm.stringWidth(hoveredObj.name);
            int tx = GameConfig.Display.GAME_WIDTH / 2 - textW / 2;
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
        drawGameEndNotification((Graphics2D) g);

        if (waitingForPlayers) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, GameConfig.Display.GAME_WIDTH, GameConfig.Display.GAME_HEIGHT);

            g.setColor(Color.WHITE);
            g.setFont(FontManager.getSmartThaiFont(24, Font.BOLD));
            FontMetrics fm = g.getFontMetrics();

            int totalPlayers = 1 + onlineCharacters.size();
            String waitingText = "รอผู้เล่นอื่น... (" + totalPlayers + "/" + GameConfig.Game.MIN_PLAYERS_TO_START + ")";
            int textWidth = fm.stringWidth(waitingText);
            int textX = (GameConfig.Display.GAME_WIDTH - textWidth) / 2;
            int textY = GameConfig.Display.GAME_HEIGHT / 2;
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

        PlayerState.Location location = objectFactory.getLocationForObject(obj);
        if (location != null && objectFactory.isInteractable(obj)) {
            moveToLocation(location, obj);
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

        moveTimer = new javax.swing.Timer(GameConfig.Game.MOVEMENT_TIMER_INTERVAL, e -> {
            if (currentPathIndex < currentPath.size()) {
                Point targetPoint = currentPath.get(currentPathIndex);
                Point currentPos = character.getPosition();
                
                if (!targetPoint.equals(currentPos)) {
                    character.setPosition(targetPoint);
                    playerState.setCurrentPosition(targetPoint);

                    if (networkClient != null && currentPathIndex % 5 == 0) {
                        networkClient.sendPlayerMove(targetPoint);
                    }
                }

                if (networkClient != null && currentPathIndex % 10 == 0) {
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
            playerState.useTime(GameConfig.Game.TIME_PER_MOVEMENT);
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
        String windowId = objectFactory.getWindowIdForLocation(location);
        GameObjectType type = objectFactory.getTypeByLocation(location);

        if (windowId != null && type != null) {
            windowManager.showWindow(windowId, type.getDisplayName());
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
    
    private GameStateManager gameStateManager;
    private CoreDataManager coreDataManager;
    
    
    private boolean isMyTurn() {
        if (!isTurnBasedMode) return true;
        
        String myPlayerId = (networkClient != null) ? networkClient.getMyPlayerData().playerId : null;
        String currentTurnPlayer = coreDataManager.getCurrentTurnPlayer();
        
        if (currentTurnPlayer == null) {
            Debug.logTurn("isMyTurn: Game not started yet, currentTurnPlayer is null");
            return false;
        }
        
        if (myPlayerId == null) {
            Debug.logTurn("isMyTurn: networkClient is null, cannot determine turn");
            return false;
        }
        
        boolean isMyTurn = currentTurnPlayer.equals(myPlayerId);
        
        if (isMyTurn) {
            Debug.logTurn("✅ เป็นเทิร์นของคุณ: " + myPlayerId);
        } else {
            Debug.logTurn("⏳ ไม่ใช่เทิร์นของคุณ - เทิร์นปัจจุบัน: " + currentTurnPlayer + ", คุณ: " + myPlayerId);
        }
        
        return isMyTurn;
    }
    
    private void updateOnlinePlayers() {
        if (networkClient == null) return;
        Map<String, PlayerData> players = networkClient.getOnlinePlayers();

        Set<String> currentPlayerIds = new HashSet<>(players.keySet());
        Set<String> characterIds = new HashSet<>(onlineCharacters.keySet());

        long currentTime = System.currentTimeMillis();

        for (String playerId : currentPlayerIds) {
            PlayerData player = players.get(playerId);
            if (player == null) {
                continue;
            }
            
            if (!onlineCharacters.containsKey(playerId)) {
                Point playerPosition = player.position != null ? player.position : GameConfig.Map.APARTMENT_POINT;
                String characterImage = player.characterImage != null ? player.characterImage : Lang.MALE_01;
                
                onlineCharacters.put(playerId, new core.Character(playerPosition, characterImage));
                lastKnownPositions.put(playerId, playerPosition);
                lastUpdateTimes.put(playerId, currentTime);
                
                PlayerState playerState = new PlayerState();
                playerState.setPlayerName(player.playerName);
                playerState.setCharacterImagePath(characterImage);
                playerState.setCurrentPosition(playerPosition);
                playerState.setMoney(player.money);
                playerState.setHealth(player.health);
                playerState.setEnergy(player.energy);
                onlineHUDManager.addPlayer(playerId, playerState);
                
              
                if (!coreDataManager.getAllPlayers().containsKey(playerId)) {
                    coreDataManager.addPlayer(playerId, player.playerName, playerPosition, characterImage);
                    Debug.logTurn("Added player to CoreDataManager: " + playerId + " (" + player.playerName + ")");
                    
                    if (coreDataManager.canStartGame()) {
                        String currentTurn = coreDataManager.getCurrentTurnPlayer();
                        if (currentTurn != null) {
                            Debug.logTurn("🎮 Game started! Current turn: " + currentTurn);
                            
                            showInitialTurnPopup();
                            
                            updateTurnFromServer();
                        } else {
                            Debug.logTurn("🎮 Game ready but no turn set yet, waiting for server...");
                        }
                    }
                } else {
                    Debug.logTurn("Player already exists in CoreDataManager: " + playerId + " (" + player.playerName + ")");
                }
                           java.util.List<String> allPlayerIds = new java.util.ArrayList<>();
                String myPlayerId = networkClient.getMyPlayerData().playerId;
                allPlayerIds.add(myPlayerId);
                allPlayerIds.addAll(networkClient.getOnlinePlayers().keySet());
                updatePlayerNumbers(allPlayerIds);
            } else {
                core.Character existingChar = onlineCharacters.get(playerId);
                if (existingChar != null) {
                    Point newPos = player.position;
                    Point lastKnownPos = lastKnownPositions.get(playerId);
                    
                    if (newPos != null && (lastKnownPos == null || !lastKnownPos.equals(newPos))) {
                        existingChar.setPosition(newPos);
                        lastKnownPositions.put(playerId, newPos);
                        lastUpdateTimes.put(playerId, currentTime);
                    }
            
                    String currentImage = existingChar.getImagePath();
                    String newImage = player.characterImage;
                    if (newImage != null && !newImage.isEmpty() && !newImage.equals(currentImage)) {
                        existingChar.updateImage(newImage);
                        onlineHUDManager.updatePlayer(playerId, player);
                    }
                    onlineHUDManager.updatePlayer(playerId, player);
                    
                    if (coreDataManager.canStartGame()) {
                        String currentTurn = coreDataManager.getCurrentTurnPlayer();
                        if (currentTurn != null) {
                            Debug.logTurn("🎮 Received turn data from server: " + currentTurn);
                            updateTurnFromServer();
                        }
                    }
                    
                    if (currentTime % 1000 < 50) { 
                        Debug.log("อัปเดตข้อมูลผู้เล่น: " + playerId + " เวลา: " + player.remainingTime + " ชั่วโมง");
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
            String currentTurn = coreDataManager.getCurrentTurnPlayer();
            onlineHUDManager.updateTurn(currentTurn);
        }
        if (characterHUD != null) {
            String myPlayerId = (networkClient != null) ? networkClient.getMyPlayerData().playerId : null;
            String currentTurn = coreDataManager.getCurrentTurnPlayer();
            characterHUD.setCurrentTurn(currentTurn != null && myPlayerId != null && currentTurn.equals(myPlayerId));
        }
    }
    
    private void onTurnChanged(String newTurnPlayerId) {
        turnPopupStartTime = System.currentTimeMillis();
        
        String playerName = getPlayerNameById(newTurnPlayerId);
        Debug.logTurn("🎯 เทิร์นเปลี่ยนเป็น: " + playerName + " (" + newTurnPlayerId + ")");
        
        String myPlayerId = (networkClient != null) ? networkClient.getMyPlayerData().playerId : null;
        if (myPlayerId != null && newTurnPlayerId.equals(myPlayerId)) {
            playerState.resetTime();
            Debug.logTurn("✅ ตอนนี้เป็นเทิร์นของคุณแล้ว! เวลาถูกรีเซ็ตเป็น " + GameConfig.Game.TURN_TIME_HOURS + " ชั่วโมง");
        } else {
            Debug.logTurn("⏳ รอเทิร์นของ: " + playerName + " - คุณไม่สามารถเดินได้");
        }
        
        coreDataManager.forceSetTurn(newTurnPlayerId);
        
        updateTurnFromServer();
        repaint();
        
        if (coreDataManager.canStartGame()) {
            showInitialTurnPopup();
        }
    }
    
    
    private void syncPlayerState() {
        if (characterHUD != null) {
            characterHUD.updatePlayerState(playerState);
        }
    }
    
    private int lastSentTime = -1;
    
    private void updatePlayerNumbers(java.util.List<String> allPlayerIds) {
        String myPlayerId = (networkClient != null) ? networkClient.getMyPlayerData().playerId : null;
        
        java.util.Collections.sort(allPlayerIds);
        
        Debug.log("อัปเดต Player Numbers - รายการผู้เล่นทั้งหมด: " + allPlayerIds);
        
        for (int i = 0; i < allPlayerIds.size(); i++) {
            String playerId = allPlayerIds.get(i);
            int playerNumber = i + 1;
            
            if (myPlayerId != null && playerId.equals(myPlayerId)) {
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
        String currentTurnPlayer = coreDataManager.getCurrentTurnPlayer();
        if (currentTurnPlayer == null) {
            int playerCount = coreDataManager.getPlayerCount();
            if (playerCount < GameConfig.Game.MIN_PLAYERS_TO_START) {
                Debug.logTurn("drawTurnPopup: Waiting for more players (" + playerCount + "/" + GameConfig.Game.MIN_PLAYERS_TO_START + ")");
                return;
            } else {
                Debug.logTurn("drawTurnPopup: Game should start but currentTurnPlayer is null");
                return;
            }
        }
        
        Debug.logTurn("drawTurnPopup: currentTurnPlayer = " + currentTurnPlayer);
        
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - turnPopupStartTime;

        if (elapsedTime > GameConfig.Game.TURN_POPUP_DURATION) return;

        float alpha = 1.0f - (float) elapsedTime / GameConfig.Game.TURN_POPUP_DURATION;
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        
        String myPlayerId = (networkClient != null) ? networkClient.getMyPlayerData().playerId : null;
        boolean isMyTurn = (myPlayerId != null) && currentTurnPlayer.equals(myPlayerId);
        
        int turnPlayerNumber = getPlayerNumber(currentTurnPlayer);

        int centerX = GameConfig.Display.GAME_WIDTH / 2;
        int centerY = GameConfig.Display.GAME_HEIGHT / 2;
        int tokenSize = 120;

        g2d.setColor(new Color(0, 0, 0, (int)(180 * alpha)));
        g2d.fillRect(0, 0, GameConfig.Display.GAME_WIDTH, GameConfig.Display.GAME_HEIGHT);
        
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
        
        Debug.logTurn("drawTurnPopup: " + turnText + " (alpha: " + alpha + ")");
    }
    
    private String getPlayerNameById(String playerId) {
        if (networkClient != null) {
            Map<String, PlayerData> players = networkClient.getOnlinePlayers();
            PlayerData player = players.get(playerId);
            if (player != null) {
                return player.playerName;
            }
        }
        
        PlayerData player = coreDataManager.getPlayer(playerId);
        if (player != null) {
            return player.playerName;
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
        int totalPlayers = coreDataManager.getPlayerCount();
        boolean shouldWait = totalPlayers < GameConfig.Game.MIN_PLAYERS_TO_START;
        boolean gameReady = totalPlayers >= GameConfig.Game.MIN_PLAYERS_TO_START;

        Debug.logTurn("checkPlayerCount: " + totalPlayers + "/" + GameConfig.Game.MIN_PLAYERS_TO_START + " players");
        
        if (waitingForPlayers != shouldWait) {
            waitingForPlayers = shouldWait;
            
            if (!waitingForPlayers && gameReady) {
                if (coreDataManager.canStartGame()) {
                    String currentTurn = coreDataManager.getCurrentTurnPlayer();
                    if (currentTurn != null) {
                        Debug.logTurn("🎮 Game started! First turn: " + currentTurn);
                        
                        showInitialTurnPopup();
                        
                        updateTurnFromServer();
                    } else {
                        Debug.logTurn("🎮 Game ready but no turn set yet, waiting for server...");
                    }
                }
            }
        }
    }
    
    private void showInitialTurnPopup() {
        turnPopupStartTime = System.currentTimeMillis();
    }
    
    private void checkTimeExpired() {
        String currentTurnPlayer = coreDataManager.getCurrentTurnPlayer();
        if (currentTurnPlayer == null) {
            Debug.logTurn("checkTimeExpired: currentTurnPlayer is null");
            return;
        }
        
        String myPlayerId = (networkClient != null) ? networkClient.getMyPlayerData().playerId : null;
        if (myPlayerId == null || !currentTurnPlayer.equals(myPlayerId)) {
            Debug.logTurn("checkTimeExpired: Not my turn, current turn: " + currentTurnPlayer + ", my ID: " + myPlayerId);
            return;
        }
        
        if (!playerState.hasTimeLeft()) {
            Debug.logTurn("⏰ เวลาหมดแล้ว! ส่งเทิร์นเสร็จสิ้นอัตโนมัติ");
            playerState.resetTime();
            sendTurnCompleteToServer();
        }
    }
    
    private void drawTimeDisplay(Graphics2D g2d) {
        if (playerState == null || waitingForPlayers) return;
        
        int currentPlayerTime = 0;
        String currentPlayerName = "";
        
        String currentTurnPlayer = coreDataManager.getCurrentTurnPlayer();
        Debug.logTurn("drawTimeDisplay: currentTurnPlayer = " + currentTurnPlayer);
        
        if (currentTurnPlayer == null) {
            int playerCount = coreDataManager.getPlayerCount();
            if (playerCount < GameConfig.Game.MIN_PLAYERS_TO_START) {
                Debug.logTurn("drawTimeDisplay: Waiting for more players (" + playerCount + "/" + GameConfig.Game.MIN_PLAYERS_TO_START + ")");
                currentPlayerTime = playerState.getRemainingTime();
                currentPlayerName = "รอผู้เล่น (" + playerCount + "/" + GameConfig.Game.MIN_PLAYERS_TO_START + ")";
            } else {
                Debug.logTurn("drawTimeDisplay: Game should start but currentTurnPlayer is null");
                currentPlayerTime = playerState.getRemainingTime();
                currentPlayerName = "คุณ (P1)";
            }
        } else {
            String myPlayerId = (networkClient != null) ? networkClient.getMyPlayerData().playerId : null;
            if (myPlayerId != null && currentTurnPlayer.equals(myPlayerId)) {
                currentPlayerTime = playerState.getRemainingTime();
                currentPlayerName = "คุณ (P" + getPlayerNumber(myPlayerId) + ")";
            } else {
                PlayerData currentPlayer = (networkClient != null) ? networkClient.getOnlinePlayers().get(currentTurnPlayer) : null;
                if (currentPlayer != null) {
                    currentPlayerTime = currentPlayer.remainingTime;
                    currentPlayerName = currentPlayer.playerName + " (P" + getPlayerNumber(currentTurnPlayer) + ")";
                } else {
                    currentPlayerTime = 24;
                    currentPlayerName = "ผู้เล่นอื่น (P" + getPlayerNumber(currentTurnPlayer) + ")";
                }
            }
        }
        
        String timeText = currentPlayerName + " - เวลาที่เหลือ: " + currentPlayerTime + " ชั่วโมง";
        
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(GameConfig.Display.GAME_WIDTH/2 - 200, GameConfig.Display.GAME_HEIGHT - 60, 400, 40, 10, 10);

        g2d.setColor(Color.WHITE);
        g2d.setFont(FontManager.getSmartThaiFont(GameConfig.Game.TIME_DISPLAY_FONT_SIZE, Font.BOLD));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(timeText);
        int textX = GameConfig.Display.GAME_WIDTH/2 - textWidth/2;
        int textY = GameConfig.Display.GAME_HEIGHT - 30;
        
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

    private void onGameEnded(String reason) {
        gameEndTime = System.currentTimeMillis();
        gameEndReason = reason;
        Debug.log("🎮 Game has ended: " + reason);

        if (moveTimer != null && moveTimer.isRunning()) {
            moveTimer.stop();
        }
        if (waitingTimer != null && waitingTimer.isRunning()) {
            waitingTimer.stop();
        }
        if (timeCheckTimer != null && timeCheckTimer.isRunning()) {
            timeCheckTimer.stop();
        }
        if (networkTimer != null && networkTimer.isRunning()) {
            networkTimer.stop();
        }

        isMoving = false;
        repaint();
    }

    private void drawGameEndNotification(Graphics2D g2d) {
        if (gameEndTime == 0) return;

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - gameEndTime;

        if (elapsedTime > GameConfig.Game.GAME_END_NOTIFICATION_DURATION) return;

        float alpha = 1.0f - (float) elapsedTime / GameConfig.Game.GAME_END_NOTIFICATION_DURATION;
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));

        int centerX = GameConfig.Display.GAME_WIDTH / 2;
        int centerY = GameConfig.Display.GAME_HEIGHT / 2;

        g2d.setColor(new Color(0, 0, 0, (int)(200 * alpha)));
        g2d.fillRect(0, 0, GameConfig.Display.GAME_WIDTH, GameConfig.Display.GAME_HEIGHT);

        g2d.setColor(new Color(255, 100, 100, (int)(255 * alpha)));
        g2d.setFont(FontManager.getSmartThaiFont(36, Font.BOLD));
        FontMetrics fm = g2d.getFontMetrics();

        String endText = "เกมจบแล้ว!";
        int textWidth = fm.stringWidth(endText);
        g2d.drawString(endText, centerX - textWidth / 2, centerY - 40);

        g2d.setColor(new Color(255, 255, 255, (int)(255 * alpha)));
        g2d.setFont(FontManager.getSmartThaiFont(20, Font.PLAIN));
        fm = g2d.getFontMetrics();

        String reasonText = gameEndReason;
        textWidth = fm.stringWidth(reasonText);
        g2d.drawString(reasonText, centerX - textWidth / 2, centerY + 10);

        String restartText = "กรุณารีสตาร์ทเซิร์ฟเวอร์";
        textWidth = fm.stringWidth(restartText);
        g2d.drawString(restartText, centerX - textWidth / 2, centerY + 40);
    }
}
