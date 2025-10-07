package test;

import core.*;
import java.awt.Point;

public class GameEndTest {

    public static void main(String[] args) {
        System.out.println("=== Game End & Player Disconnect Test ===\n");

        GameEndTest test = new GameEndTest();
        int passed = 0;
        int total = 0;

        total++; if (test.testMinPlayersToStart()) passed++;
        total++; if (test.testMinPlayersToContinue()) passed++;
        total++; if (test.testGameEndWith1Player()) passed++;
        total++; if (test.testGameContinuesWith2Players()) passed++;
        total++; if (test.testGameEndCallback()) passed++;

        System.out.println("\n=== Test Results ===");
        System.out.println("Passed: " + passed + "/" + total);
        System.out.println(passed == total ? "✅ ALL TESTS PASSED" : "❌ SOME TESTS FAILED");

        System.exit(passed == total ? 0 : 1);
    }

    public boolean testMinPlayersToStart() {
        System.out.println("Test 1: Minimum Players to Start (3 players)");
        try {
            CoreDataManager cdm = CoreDataManager.getInstance();
            cdm.reset();

            cdm.addPlayer("p1", "Player1", new Point(0, 0), "c1.png");
            assert !cdm.canStartGame() : "Cannot start with 1 player";

            cdm.addPlayer("p2", "Player2", new Point(0, 0), "c2.png");
            assert !cdm.canStartGame() : "Cannot start with 2 players";

            cdm.addPlayer("p3", "Player3", new Point(0, 0), "c3.png");
            assert cdm.canStartGame() : "Can start with 3 players";

            System.out.println("  ✓ Requires exactly 3 players to start");

            cdm.reset();
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ Failed: " + e.getMessage());
            return false;
        }
    }

    public boolean testMinPlayersToContinue() {
        System.out.println("\nTest 2: Minimum Players to Continue (2 players)");
        try {
            CoreDataManager cdm = CoreDataManager.getInstance();
            cdm.reset();

            cdm.addPlayer("p1", "Player1", new Point(0, 0), "c1.png");
            cdm.addPlayer("p2", "Player2", new Point(0, 0), "c2.png");
            cdm.addPlayer("p3", "Player3", new Point(0, 0), "c3.png");

            Thread.sleep(100);
            cdm.startGame();

            assert cdm.hasGameStarted() : "Game should have started";
            assert cdm.getCurrentPhase() == CoreDataManager.GamePhase.GAME_RUNNING : "Game should be running";

            cdm.removePlayer("p3");

            assert cdm.getPlayerCount() == 2 : "Should have 2 players";
            assert !cdm.isGameEnded() : "Game should NOT end with 2 players";
            assert cdm.getCurrentPhase() == CoreDataManager.GamePhase.GAME_RUNNING : "Game should still be running";

            System.out.println("  ✓ Game continues with exactly 2 players");

            cdm.reset();
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ Failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean testGameEndWith1Player() {
        System.out.println("\nTest 3: Game Ends with 1 Player");
        try {
            CoreDataManager cdm = CoreDataManager.getInstance();
            cdm.reset();

            cdm.addPlayer("p1", "Player1", new Point(0, 0), "c1.png");
            cdm.addPlayer("p2", "Player2", new Point(0, 0), "c2.png");
            cdm.addPlayer("p3", "Player3", new Point(0, 0), "c3.png");

            Thread.sleep(100);
            cdm.startGame();

            cdm.removePlayer("p3");
            assert !cdm.isGameEnded() : "Game should continue with 2 players";

            cdm.removePlayer("p2");

            assert cdm.getPlayerCount() == 1 : "Should have 1 player";
            assert cdm.isGameEnded() : "Game SHOULD end with 1 player";
            assert cdm.getCurrentPhase() == CoreDataManager.GamePhase.GAME_ENDED : "Phase should be GAME_ENDED";

            System.out.println("  ✓ Game properly ends when down to 1 player");

            cdm.reset();
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ Failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean testGameContinuesWith2Players() {
        System.out.println("\nTest 4: Verify Bug Fix (Game continues with 2 players)");
        try {
            CoreDataManager cdm = CoreDataManager.getInstance();
            cdm.reset();

            cdm.addPlayer("p1", "Player1", new Point(0, 0), "c1.png");
            cdm.addPlayer("p2", "Player2", new Point(0, 0), "c2.png");
            cdm.addPlayer("p3", "Player3", new Point(0, 0), "c3.png");
            cdm.addPlayer("p4", "Player4", new Point(0, 0), "c4.png");

            Thread.sleep(100);
            cdm.startGame();

            String turn1 = cdm.getCurrentTurnPlayer();
            assert turn1 != null : "Turn should be set";

            cdm.removePlayer("p3");
            assert cdm.getPlayerCount() == 3 : "3 players remaining";
            assert !cdm.isGameEnded() : "Game continues with 3";

            cdm.removePlayer("p4");
            assert cdm.getPlayerCount() == 2 : "2 players remaining";
            assert !cdm.isGameEnded() : "Game continues with 2 (BUG FIX VERIFIED)";

            String currentTurn = cdm.getCurrentTurnPlayer();
            assert currentTurn != null : "Turn should still be active";

            System.out.println("  ✓ Bug fix verified: Game continues with exactly 2 players");
            System.out.println("  ✓ Turn system still works with 2 players");

            cdm.reset();
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ Failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean testGameEndCallback() {
        System.out.println("\nTest 5: Game End Callback");
        try {
            CoreDataManager cdm = CoreDataManager.getInstance();
            cdm.reset();

            final boolean[] callbackCalled = {false};
            final String[] callbackReason = {null};

            cdm.setGameEndCallback(reason -> {
                callbackCalled[0] = true;
                callbackReason[0] = reason;
            });

            cdm.addPlayer("p1", "Player1", new Point(0, 0), "c1.png");
            cdm.addPlayer("p2", "Player2", new Point(0, 0), "c2.png");
            cdm.addPlayer("p3", "Player3", new Point(0, 0), "c3.png");

            Thread.sleep(100);
            cdm.startGame();

            cdm.removePlayer("p3");
            assert !callbackCalled[0] : "Callback should NOT fire with 2 players";

            cdm.removePlayer("p2");
            assert callbackCalled[0] : "Callback should fire when game ends";
            assert callbackReason[0] != null : "Callback should have reason";

            System.out.println("  ✓ Game end callback works");
            System.out.println("  ✓ Callback reason: " + callbackReason[0]);

            cdm.reset();
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ Failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
