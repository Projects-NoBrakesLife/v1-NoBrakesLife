package core;

import java.awt.Point;

/**
 * อินเตอร์เฟสสำหรับการซิงค์ข้อมูลระหว่าง Client และ Server
 * ช่วยให้การส่งข้อมูลเป็นระเบียบและไม่ซ้ำซ้อน
 */
public interface DataSyncManager {
    
    /**
     * ตรวจสอบว่าควรส่งข้อมูลสถิติหรือไม่
     */
    boolean shouldSyncStats(String playerId, int money, int health, int energy);
    
    /**
     * ตรวจสอบว่าควรส่งข้อมูลตำแหน่งหรือไม่
     */
    boolean shouldSyncPosition(String playerId, Point position);
    
    /**
     * ตรวจสอบว่าควรส่งข้อมูลเวลาหรือไม่
     */
    boolean shouldSyncTime(String playerId, int remainingTime);
    
    /**
     * ตรวจสอบว่าควรส่งข้อมูลสถานที่หรือไม่
     */
    boolean shouldSyncLocation(String playerId, PlayerState.Location location);
    
    /**
     * อัปเดตข้อมูลสถิติที่ส่งล่าสุด
     */
    void updateLastStatsSync(String playerId, int money, int health, int energy);
    
    /**
     * อัปเดตข้อมูลตำแหน่งที่ส่งล่าสุด
     */
    void updateLastPositionSync(String playerId, Point position);
    
    /**
     * อัปเดตข้อมูลเวลาที่ส่งล่าสุด
     */
    void updateLastTimeSync(String playerId, int remainingTime);
    
    /**
     * อัปเดตข้อมูลสถานที่ที่ส่งล่าสุด
     */
    void updateLastLocationSync(String playerId, PlayerState.Location location);
    
    /**
     * ลบข้อมูลผู้เล่น
     */
    void removePlayer(String playerId);
    
    /**
     * รีเซ็ตข้อมูลทั้งหมด
     */
    void reset();
}
