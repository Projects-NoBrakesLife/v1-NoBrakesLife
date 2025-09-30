package core;

import java.awt.Point;

public class PlayerState {
    public enum Location {
        APARTMENT_SHITTY,
        BANK,
        TECH,
        JOB_OFFICE,
        CLOCK_TOWER,
        SHOP,
        HOSPITAL
    }

    private Location currentLocation;
    private Point currentPosition;
    private String playerName;
    private int money;
    private int health;
    private int energy;

    public PlayerState() {
        this.currentLocation = Location.APARTMENT_SHITTY;
        this.currentPosition = new Point(878, 300);
        this.playerName = "Player";
        this.money = 1000;
        this.health = 100;
        this.energy = 100;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Location location) {
        this.currentLocation = location;
        System.out.println("Player moved to: " + location);
    }

    public Point getCurrentPosition() {
        return new Point(currentPosition);
    }

    public void setCurrentPosition(Point position) {
        this.currentPosition = new Point(position);
        System.out.println("Player position set to: " + position);
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String name) {
        this.playerName = name;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public void addMoney(int amount) {
        this.money += amount;
        System.out.println("Money added: " + amount + ", Total: " + this.money);
    }

    public boolean spendMoney(int amount) {
        if (this.money >= amount) {
            this.money -= amount;
            System.out.println("Money spent: " + amount + ", Remaining: " + this.money);
            return true;
        }
        return false;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = Math.max(0, Math.min(100, health));
    }

    public void addHealth(int amount) {
        setHealth(this.health + amount);
        System.out.println("Health added: " + amount + ", Current: " + this.health);
    }

    public void takeDamage(int damage) {
        setHealth(this.health - damage);
        System.out.println("Damage taken: " + damage + ", Health: " + this.health);
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = Math.max(0, Math.min(100, energy));
    }

    public void addEnergy(int amount) {
        setEnergy(this.energy + amount);
        System.out.println("Energy added: " + amount + ", Current: " + this.energy);
    }

    public void useEnergy(int amount) {
        setEnergy(this.energy - amount);
        System.out.println("Energy used: " + amount + ", Remaining: " + this.energy);
    }

    public void rest() {
        addHealth(20);
        addEnergy(30);
        System.out.println("Player rested - Health: " + health + ", Energy: " + energy);
    }

    public String getStatusString() {
        return String.format("Location: %s | Position: (%d, %d) | Money: %d | Health: %d | Energy: %d",
                currentLocation, currentPosition.x, currentPosition.y, money, health, energy);
    }

    public void printStatus() {
        System.out.println("=== Player Status ===");
        System.out.println("Name: " + playerName);
        System.out.println("Location: " + currentLocation);
        System.out.println("Position: " + currentPosition);
        System.out.println("Money: " + money);
        System.out.println("Health: " + health);
        System.out.println("Energy: " + energy);
        System.out.println("===================");
    }
}
