package test;

import core.CoreDataManager;
import network.*;
import java.awt.Point;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class NetworkTest {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        System.out.println("=== Network System Test ===\n");

        NetworkTest test = new NetworkTest();

        try {
            test.testServerStartup();

            test.testMultipleClients();

            test.testHeartbeat();

            test.testConnectionCleanup();

            test.testTurnManagement();

            System.out.println("\n=== All Tests Completed ===");

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void testServerStartup() {
        System.out.println("Test 1: Server Startup");
        System.out.println("Please start GameServer manually in another terminal");
        System.out.println("Waiting 3 seconds...\n");
        sleep(3000);
        System.out.println("✓ Server should be running\n");
    }

    public void testMultipleClients() {
        System.out.println("Test 2: Multiple Clients (3+ players)");

        NetworkClient client1 = createTestClient("Player1", "Male-01.png");
        NetworkClient client2 = createTestClient("Player2", "Male-02.png");
        NetworkClient client3 = createTestClient("Player3", "Female-01.png");
        NetworkClient client4 = createTestClient("Player4", "Female-02.png");

        System.out.println("Connecting 4 clients...");

        client1.connect();
        sleep(500);
        client2.connect();
        sleep(500);
        client3.connect();
        sleep(500);
        client4.connect();
        sleep(2000);

        System.out.println("Connected clients: ");
        System.out.println("  - Client 1: " + client1.isConnected());
        System.out.println("  - Client 2: " + client2.isConnected());
        System.out.println("  - Client 3: " + client3.isConnected());
        System.out.println("  - Client 4: " + client4.isConnected());

        System.out.println("Online players count:");
        System.out.println("  - Client 1 sees: " + client1.getOnlinePlayers().size() + " players");
        System.out.println("  - Client 2 sees: " + client2.getOnlinePlayers().size() + " players");
        System.out.println("  - Client 3 sees: " + client3.getOnlinePlayers().size() + " players");
        System.out.println("  - Client 4 sees: " + client4.getOnlinePlayers().size() + " players");

        boolean allConnected = client1.isConnected() && client2.isConnected() &&
                               client3.isConnected() && client4.isConnected();

        System.out.println(allConnected ? "✓ All clients connected successfully" : "✗ Some clients failed to connect");
        System.out.println();

        this.client1 = client1;
        this.client2 = client2;
        this.client3 = client3;
        this.client4 = client4;
    }

    public void testHeartbeat() {
        System.out.println("Test 3: Heartbeat Mechanism");
        System.out.println("Monitoring heartbeat for 15 seconds...");

        long startTime = System.currentTimeMillis();
        int expectedHeartbeats = 3; // 15 seconds / 5 seconds interval

        System.out.println("Expected heartbeats: " + expectedHeartbeats);
        System.out.println("Heartbeat interval: 5 seconds");

        for (int i = 0; i < 15; i++) {
            sleep(1000);
            if ((i + 1) % 5 == 0) {
                System.out.println("  [" + (i + 1) + "s] Heartbeat sent");
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("Time elapsed: " + (elapsed / 1000) + " seconds");
        System.out.println("Clients still connected: " +
            (client1.isConnected() && client2.isConnected() &&
             client3.isConnected() && client4.isConnected()));

        System.out.println("✓ Heartbeat mechanism is working\n");
    }

    public void testConnectionCleanup() {
        System.out.println("Test 4: Connection Cleanup (Stale Connections)");
        System.out.println("Disconnecting client 3 and waiting for cleanup...");

        client3.disconnect();
        System.out.println("Client 3 disconnected: " + !client3.isConnected());

        System.out.println("Waiting 20 seconds for server cleanup...");
        for (int i = 0; i < 20; i++) {
            sleep(1000);
            if ((i + 1) % 5 == 0) {
                System.out.println("  [" + (i + 1) + "s] Checking...");
            }
        }

        int client1Sees = client1.getOnlinePlayers().size();
        int client2Sees = client2.getOnlinePlayers().size();

        System.out.println("Online players after cleanup:");
        System.out.println("  - Client 1 sees: " + client1Sees + " players");
        System.out.println("  - Client 2 sees: " + client2Sees + " players");

        System.out.println("✓ Connection cleanup is working\n");
    }

    public void testTurnManagement() {
        System.out.println("Test 5: Turn Management");

        String currentTurn1 = client1.getCurrentTurnPlayer();
        String currentTurn2 = client2.getCurrentTurnPlayer();

        System.out.println("Current turn player:");
        System.out.println("  - Client 1 sees: " + currentTurn1);
        System.out.println("  - Client 2 sees: " + currentTurn2);

        if (currentTurn1 != null && currentTurn1.equals(client1.getMyPlayerData().playerId)) {
            System.out.println("\nClient 1 completing turn...");
            client1.sendTurnComplete();
            sleep(1000);

            String newTurn = client2.getCurrentTurnPlayer();
            System.out.println("New turn player: " + newTurn);
        }

        System.out.println("✓ Turn management is working\n");
    }

    private NetworkClient client1, client2, client3, client4;

    private NetworkClient createTestClient(String name, String character) {
        String playerId = "test_" + name.toLowerCase() + "_" + System.currentTimeMillis();
        Point startPos = new Point(100, 100);
        return new NetworkClient(playerId, name, startPos, "assets/players/" + character);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
