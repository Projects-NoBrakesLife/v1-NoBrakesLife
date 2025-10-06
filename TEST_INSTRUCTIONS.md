# Network System Test Instructions

## Overview
คู่มือการทดสอบระบบ Network ของเกม รวมถึง heartbeat mechanism, connection cleanup, และการจัดการ multiple players

## Requirements
- Java 11 or higher
- Git (for version info)
- Terminal หรือ Command Prompt

## Test Components

### 1. NetworkTest.java
ตัวทดสอบอัตโนมัติที่จำลองการทำงานของระบบ network

**ฟีเจอร์ที่ทดสอบ:**
- ✅ Multiple clients (3+ players)
- ✅ Heartbeat mechanism (ทุก 5 วินาที)
- ✅ Connection cleanup (timeout 15 วินาที)
- ✅ Turn management
- ✅ Player synchronization

## How to Run Tests

### Step 1: Compile All Files
```bash
# Compile all source files
find src -name "*.java" -exec javac -d bin -cp "lib/*" {} +

# Compile test
javac -d bin -cp "lib/*:bin" src/test/NetworkTest.java
```

### Step 2: Start Game Server
เปิด Terminal ใหม่และรัน:
```bash
java -cp bin network.GameServer
```

**Expected:**
- หน้าต่าง "Game Server - Debug" จะเปิดขึ้น
- กด "Start Server" button
- Server จะฟัง connection ที่ port 12345

### Step 3: Run Network Test
เปิด Terminal อีกอันและรัน:
```bash
java -cp bin test.NetworkTest
```

## Test Results Interpretation

### Test 1: Server Startup
```
Test 1: Server Startup
Please start GameServer manually in another terminal
Waiting 3 seconds...
✓ Server should be running
```
**ผ่าน:** ถ้า Server window เปิดและแสดง "Server started on port 12345"

### Test 2: Multiple Clients (3+ players)
```
Test 2: Multiple Clients (3+ players)
Connecting 4 clients...
Connected clients:
  - Client 1: true
  - Client 2: true
  - Client 3: true
  - Client 4: true
Online players count:
  - Client 1 sees: 3 players
  - Client 2 sees: 3 players
  - Client 3 sees: 3 players
  - Client 4 sees: 3 players
✓ All clients connected successfully
```
**ผ่าน:**
- ทุก client connected = true
- แต่ละ client เห็น player อื่น 3 คน (ไม่นับตัวเอง)
- Server window แสดง "Players: 4"

### Test 3: Heartbeat Mechanism
```
Test 3: Heartbeat Mechanism
Monitoring heartbeat for 15 seconds...
Expected heartbeats: 3
Heartbeat interval: 5 seconds
  [5s] Heartbeat sent
  [10s] Heartbeat sent
  [15s] Heartbeat sent
Time elapsed: 15 seconds
Clients still connected: true
✓ Heartbeat mechanism is working
```
**ผ่าน:**
- Heartbeat ส่งทุก 5 วินาที
- ไม่มี client ถูก disconnect
- Server log ไม่แสดง "stale connection"

### Test 4: Connection Cleanup (Stale Connections)
```
Test 4: Connection Cleanup (Stale Connections)
Disconnecting client 3 and waiting for cleanup...
Client 3 disconnected: true
Waiting 20 seconds for server cleanup...
  [5s] Checking...
  [10s] Checking...
  [15s] Checking...
  [20s] Checking...
Online players after cleanup:
  - Client 1 sees: 2 players
  - Client 2 sees: 2 players
✓ Connection cleanup is working
```
**ผ่าน:**
- Client 3 disconnect สำเร็จ
- หลัง 15+ วินาที server ลบ client 3 ออก
- Client อื่นเห็น player ลดลงเป็น 2 คน
- Server log แสดง "Removing stale connection" หรือ "Player left"

### Test 5: Turn Management
```
Test 5: Turn Management
Current turn player:
  - Client 1 sees: test_player1_...
  - Client 2 sees: test_player1_...

Client 1 completing turn...
New turn player: test_player2_...
✓ Turn management is working
```
**ผ่าน:**
- ทุก client เห็น current turn player เหมือนกัน
- เมื่อ complete turn, turn จะเปลี่ยนไปคนถัดไป
- Server broadcast turn change ไปทุก client

## Expected Server Logs

### Normal Operation
```
[HH:mm:ss] Server started on port 12345
[HH:mm:ss] Cleanup timer started
[HH:mm:ss] Client connected: /127.0.0.1:xxxxx
[HH:mm:ss] Player joined: Player1 (P1) at java.awt.Point[x=100,y=100]
[HH:mm:ss] Total players now: 1
[HH:mm:ss] 🎮 Starting game with 3 players
```

### Heartbeat Working
```
# No logs - heartbeat works silently
# Server tracks playerLastSeen timestamps internally
```

### Cleanup Working
```
[HH:mm:ss] Removing stale connection: P3 (test_player3_...)
[HH:mm:ss] Player left: P3
```

## Common Issues

### Issue 1: "Connection refused"
**Cause:** Server ไม่ได้เปิด หรือเปิดไม่ทัน
**Fix:**
- ตรวจสอบว่า GameServer รันอยู่
- รอ 3-5 วินาทีหลัง start server ก่อนรัน test

### Issue 2: "Clients see 0 players"
**Cause:** Network synchronization delay
**Fix:**
- เพิ่มเวลารอใน test (sleep 1-2 วินาที)
- ตรวจสอบว่า server broadcast ข้อมูล

### Issue 3: "Cleanup not working"
**Cause:** Heartbeat ยังส่งอยู่
**Fix:**
- ตรวจสอบว่า client disconnect จริงๆ
- เพิ่มเวลารอเกิน CONNECTION_TIMEOUT (15 วินาที)

### Issue 4: "Turn not changing"
**Cause:** Game phase ไม่ถูกต้อง
**Fix:**
- ต้องมี 3+ players ถึงจะ start game
- ตรวจสอบ CoreDataManager.currentPhase

## Performance Metrics

### Expected Values
- **Connection time:** < 500ms per client
- **Heartbeat interval:** 5000ms ± 100ms
- **Cleanup timeout:** 15000ms ± 500ms
- **Turn broadcast:** < 100ms
- **Player sync:** < 200ms

### System Resources
- **Memory:** ~50MB per client
- **Network:** ~1KB/s per client (idle)
- **CPU:** < 5% total

## Manual Testing

หากต้องการทดสอบแบบ manual:

### Test Heartbeat
1. Start server
2. Connect 1 client
3. Wait 30 seconds
4. Check: Client should still be connected

### Test Cleanup
1. Start server
2. Connect 1 client
3. Kill client process (force quit)
4. Wait 20 seconds
5. Check: Server should remove player

### Test Multiple Players
1. Start server
2. Open 4 different terminals
3. In each terminal, start a game client
4. Check: All 4 players should see each other

## Troubleshooting

### Debug Mode
เปิด debug logging:
```java
// In CoreDataManager.java
private static final boolean DEBUG = true;
```

### Network Monitoring
ใช้ wireshark หรือ tcpdump:
```bash
tcpdump -i lo0 -A port 12345
```

### Heap Dump
ตรวจสอบ memory leaks:
```bash
jmap -dump:live,format=b,file=heap.bin <pid>
```

## Success Criteria

✅ **Pass Criteria:**
- [ ] All 5 tests show ✓ symbol
- [ ] No exceptions in console
- [ ] Server handles 4+ concurrent clients
- [ ] Heartbeat prevents disconnection for 30+ seconds
- [ ] Cleanup removes inactive players within 20 seconds
- [ ] Turn management works correctly
- [ ] No memory leaks after 100 connects/disconnects

## Version Info

Test ใช้ version info จาก git:
```bash
java -cp bin util.VersionInfo
```

Current test version: **v1.0.34+**

## Support

หากพบปัญหา:
1. ตรวจสอบ server logs
2. ดู TEST_INSTRUCTIONS.md
3. รัน test ใหม่หลัง restart server
4. ตรวจสอบ network/firewall settings
