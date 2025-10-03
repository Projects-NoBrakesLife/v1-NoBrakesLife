package core;

import java.awt.Point;

public class PlayerState {
    public enum Location {
        APARTMENT_SHITTY,
        BANK,
        TECH,
        JOB_OFFICE,
        CULTURE,
        UNIVERSITY
     
    }

    private Location currentLocation;
    private Point currentPosition;
    private String playerName;
    private String characterImagePath;
    private int money;
    private int health;
    private int energy;

    public PlayerState() {
        this.currentLocation = Location.APARTMENT_SHITTY;
        this.currentPosition = new Point(878, 300);
        this.playerName = Lang.DEFAULT_PLAYER_NAME;
        this.characterImagePath = Lang.MALE_01;
        this.money = Config.STARTING_MONEY;
        this.health = Config.STARTING_HEALTH;
        this.energy = Config.STARTING_ENERGY;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Location location) {
        this.currentLocation = location;
        Debug.log(Lang.PLAYER_MOVED_TO + location);
    }

    public Point getCurrentPosition() {
        return new Point(currentPosition);
    }

    public void setCurrentPosition(Point position) {
        if (currentPosition == null || !currentPosition.equals(position)) {
            this.currentPosition = new Point(position);
            Debug.log(Lang.PLAYER_POSITION_SET + position);
        }
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String name) {
        this.playerName = name;
    }

    public String getCharacterImagePath() {
        return characterImagePath;
    }

    public void setCharacterImagePath(String characterImagePath) {
        this.characterImagePath = characterImagePath;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public void addMoney(int amount) {
        this.money += amount;
        Debug.log(Lang.MONEY_ADDED + amount + Lang.TOTAL + this.money);
    }

    public boolean spendMoney(int amount) {
        if (this.money >= amount) {
            this.money -= amount;
            Debug.log(Lang.MONEY_SPENT + amount + Lang.REMAINING + this.money);
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
        Debug.log(Lang.HEALTH_ADDED + amount + Lang.HEALTH_CURRENT + this.health);
    }

    public void takeDamage(int damage) {
        setHealth(this.health - damage);
        Debug.log(Lang.DAMAGE_TAKEN + damage + Lang.HEALTH_STATUS + this.health);
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = Math.max(0, Math.min(100, energy));
    }

    public void addEnergy(int amount) {
        setEnergy(this.energy + amount);
        Debug.log(Lang.ENERGY_ADDED + amount + Lang.HEALTH_CURRENT + this.energy);
    }

    public void useEnergy(int amount) {
        setEnergy(this.energy - amount);
        Debug.log(Lang.ENERGY_USED + amount + Lang.REMAINING + this.energy);
    }

    public void rest() {
        addHealth(Config.REST_HEALTH_GAIN);
        addEnergy(Config.REST_ENERGY_GAIN);
        Debug.log(Lang.PLAYER_RESTED + health + Lang.ENERGY_STATUS + energy);
    }

    public String getStatusString() {
        return String.format(Lang.LOCATION_FORMAT,
                currentLocation, currentPosition.x, currentPosition.y, money, health, energy);
    }

    public void printStatus() {
        Debug.log(Lang.PLAYER_STATUS_HEADER);
        Debug.log(Lang.PLAYER_NAME + playerName);
        Debug.log(Lang.PLAYER_LOCATION + currentLocation);
        Debug.log(Lang.PLAYER_POSITION + currentPosition);
        Debug.log(Lang.PLAYER_MONEY + money);
        Debug.log(Lang.PLAYER_HEALTH + health);
        Debug.log(Lang.PLAYER_ENERGY + energy);
        Debug.log(Lang.PLAYER_STATUS_FOOTER);
    }
}
