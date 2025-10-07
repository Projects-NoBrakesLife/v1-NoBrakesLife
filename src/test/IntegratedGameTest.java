package test;

import core.*;
import network.*;
import java.awt.Point;

public class IntegratedGameTest {

    public static void main(String[] args) {
        System.out.println("=== Integrated Game Test ===\n");

        IntegratedGameTest test = new IntegratedGameTest();
        int passed = 0;
        int total = 0;

        total++; if (test.testCoreDataManager()) passed++;
        total++; if (test.testGameConfig()) passed++;
        total++; if (test.testGameObjectFactory()) passed++;
        total++; if (test.testPlayerState()) passed++;
        total++; if (test.testGamePhases()) passed++;
        total++; if (test.testTurnSystem()) passed++;

        System.out.println("\n=== Test Results ===");
        System.out.println("Passed: " + passed + "/" + total);
        System.out.println(passed == total ? "✅ ALL TESTS PASSED" : "❌ SOME TESTS FAILED");

        System.exit(passed == total ? 0 : 1);
    }

    public boolean testCoreDataManager() {
        System.out.println("Test 1: CoreDataManager");
        try {
            CoreDataManager cdm = CoreDataManager.getInstance();
            cdm.reset();

            PlayerData p1 = cdm.addPlayer("p1", "Player1", new Point(100, 100), "char1.png");
            PlayerData p2 = cdm.addPlayer("p2", "Player2", new Point(200, 200), "char2.png");

            assert cdm.getPlayerCount() == 2 : "Player count should be 2";
            assert cdm.getPlayer("p1") != null : "Player 1 should exist";
            assert cdm.getCurrentTurnPlayer() == null : "Game not started yet";

            PlayerData p3 = cdm.addPlayer("p3", "Player3", new Point(300, 300), "char3.png");
            assert cdm.canStartGame() : "Should be able to start with 3 players";

            System.out.println("  ✓ Player management works");
            System.out.println("  ✓ Game can start with 3+ players");

            cdm.reset();
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ Failed: " + e.getMessage());
            return false;
        }
    }

    public boolean testGameConfig() {
        System.out.println("\nTest 2: GameConfig");
        try {
            assert GameConfig.Game.MIN_PLAYERS_TO_START == 3 : "MIN_PLAYERS_TO_START should be 3";
            assert GameConfig.Game.MIN_PLAYERS_TO_CONTINUE == 2 : "MIN_PLAYERS_TO_CONTINUE should be 2";
            assert GameConfig.Game.TURN_TIME_HOURS == 24 : "TURN_TIME_HOURS should be 24";
            assert GameConfig.Network.SERVER_PORT == 12345 : "SERVER_PORT should be 12345";

            System.out.println("  ✓ All config values correct");
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ Failed: " + e.getMessage());
            return false;
        }
    }

    public boolean testGameObjectFactory() {
        System.out.println("\nTest 3: GameObjectFactory");
        try {
            GameObjectFactory factory = GameObjectFactory.getInstance();

            assert factory.getAllObjects().size() > 0 : "Should have objects";

            GameObject aptObj = factory.getObject(GameObjectType.APARTMENT);
            assert aptObj != null : "APARTMENT object should exist";
            assert aptObj.name.equals(Lang.APARTMENT_NAME) : "Name should match";

            PlayerState.Location loc = factory.getLocationForObject(aptObj);
            assert loc == PlayerState.Location.APARTMENT_SHITTY : "Location should match";

            String windowId = factory.getWindowIdForLocation(PlayerState.Location.BANK);
            assert windowId != null : "Window ID should exist for BANK";

            System.out.println("  ✓ Object factory works correctly");
            System.out.println("  ✓ Location mapping works");
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ Failed: " + e.getMessage());
            return false;
        }
    }

    public boolean testPlayerState() {
        System.out.println("\nTest 4: PlayerState");
        try {
            PlayerState ps = new PlayerState();

            assert ps.getMoney() == GameConfig.Character.STARTING_MONEY : "Starting money correct";
            assert ps.getHealth() == GameConfig.Character.STARTING_HEALTH : "Starting health correct";
            assert ps.getEnergy() == GameConfig.Character.STARTING_ENERGY : "Starting energy correct";
            assert ps.getRemainingTime() == GameConfig.Game.TURN_TIME_HOURS : "Starting time correct";

            ps.addMoney(100);
            assert ps.getMoney() == GameConfig.Character.STARTING_MONEY + 100 : "Money addition works";

            boolean spent = ps.spendMoney(50);
            assert spent : "Should be able to spend money";
            assert ps.getMoney() == GameConfig.Character.STARTING_MONEY + 50 : "Money deduction works";

            ps.useTime(10);
            assert ps.getRemainingTime() == GameConfig.Game.TURN_TIME_HOURS - 10 : "Time usage works";

            System.out.println("  ✓ Player state initialization correct");
            System.out.println("  ✓ Money operations work");
            System.out.println("  ✓ Time management works");
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ Failed: " + e.getMessage());
            return false;
        }
    }

    public boolean testGamePhases() {
        System.out.println("\nTest 5: Game Phases");
        try {
            CoreDataManager cdm = CoreDataManager.getInstance();
            cdm.reset();

            assert cdm.getCurrentPhase() == CoreDataManager.GamePhase.WAITING_FOR_PLAYERS : "Initial phase correct";

            cdm.addPlayer("p1", "Player1", new Point(0, 0), "c1.png");
            cdm.addPlayer("p2", "Player2", new Point(0, 0), "c2.png");
            assert cdm.isWaitingForPlayers() : "Should be waiting with 2 players";

            cdm.addPlayer("p3", "Player3", new Point(0, 0), "c3.png");
            Thread.sleep(2100);
            assert !cdm.isWaitingForPlayers() : "Should not be waiting with 3 players";

            System.out.println("  ✓ Game phases work correctly");

            cdm.reset();
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ Failed: " + e.getMessage());
            return false;
        }
    }

    public boolean testTurnSystem() {
        System.out.println("\nTest 6: Turn System");
        try {
            CoreDataManager cdm = CoreDataManager.getInstance();
            cdm.reset();

            cdm.addPlayer("p1", "Player1", new Point(0, 0), "c1.png");
            cdm.addPlayer("p2", "Player2", new Point(0, 0), "c2.png");
            cdm.addPlayer("p3", "Player3", new Point(0, 0), "c3.png");

            Thread.sleep(100);
            cdm.startGame();

            String firstTurn = cdm.getCurrentTurnPlayer();
            assert firstTurn != null : "Turn should be set";
            assert cdm.isPlayerTurn(firstTurn) : "First player turn check works";

            cdm.nextTurn();
            String secondTurn = cdm.getCurrentTurnPlayer();
            assert !secondTurn.equals(firstTurn) : "Turn should change";

            cdm.completeTurn(secondTurn);
            String thirdTurn = cdm.getCurrentTurnPlayer();
            assert !thirdTurn.equals(secondTurn) : "Turn complete works";

            System.out.println("  ✓ Turn initialization works");
            System.out.println("  ✓ Turn rotation works");
            System.out.println("  ✓ Turn completion works");

            cdm.reset();
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ Failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
