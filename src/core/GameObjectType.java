package core;

import java.awt.Point;

/**
 * Enum defining all game object types with their properties
 * Makes it easy to add new objects and manage existing ones
 */
public enum GameObjectType {
    APARTMENT(
        "./assets/maps/obj/Apartment_Shitty-0.png",
        "./assets/maps/obj/Apartment_Shitty-1.png",
        Lang.APARTMENT_NAME,
        727, 39, 152, 192, 0,
        PlayerState.Location.APARTMENT_SHITTY,
        GameConfig.Map.APARTMENT_POINT,
        Lang.APARTMENT_WINDOW,
        true
    ),

    BANK(
        "./assets/maps/obj/Bank-0.png",
        "./assets/maps/obj/Bank-1.png",
        Lang.BANK_NAME,
        1131, 291, 181, 132, 90,
        PlayerState.Location.BANK,
        GameConfig.Map.BANK_POINT,
        Lang.BANK_WINDOW,
        true
    ),

    TECH(
        "./assets/maps/obj/Tech-0.png",
        "./assets/maps/obj/Tech-1.png",
        Lang.TECH_NAME,
        1259, 471, 143, 150, 0,
        PlayerState.Location.TECH,
        GameConfig.Map.TECH_POINT,
        Lang.TECH_WINDOW,
        true
    ),

    JOB_OFFICE(
        "./assets/maps/obj/Job_Office-0.png",
        "./assets/maps/obj/Job_Office-1.png",
        Lang.JOB_OFFICE_NAME,
        911, 466, 208, 176, 0,
        PlayerState.Location.JOB_OFFICE,
        GameConfig.Map.JOB_OFFICE_POINT,
        Lang.JOB_WINDOW,
        true
    ),

    CULTURE(
        "./assets/maps/obj/Culture-0.png",
        "./assets/maps/obj/Culture-1.png",
        Lang.CULTURE_NAME,
        711, 462, 186, 105, 0,
        PlayerState.Location.CULTURE,
        GameConfig.Map.CULTURE_POINT,
        Lang.CULTURE_WINDOW,
        true
    ),

    UNIVERSITY(
        "./assets/maps/obj/University-0.png",
        "./assets/maps/obj/University-1.png",
        Lang.UNIVERSITY_NAME,
        462, 489, 168, 129, 0,
        PlayerState.Location.UNIVERSITY,
        GameConfig.Map.UNIVERSITY_POINT,
        Lang.UNIVERSITY_WINDOW,
        true
    ),

    CLOCK_TOWER(
        "./assets/ui/clock/Clock-Tower.png",
        null,
        Lang.CLOCK_TOWER_NAME,
        633, 615, 336, 352, 0,
        null,
        null,
        null,
        false
    );

    private final String normalImagePath;
    private final String hoverImagePath;
    private final String displayName;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int rotation;
    private final PlayerState.Location location;
    private final Point destinationPoint;
    private final String windowId;
    private final boolean isInteractable;

    GameObjectType(String normalImagePath, String hoverImagePath, String displayName,
                   int x, int y, int width, int height, int rotation,
                   PlayerState.Location location, Point destinationPoint,
                   String windowId, boolean isInteractable) {
        this.normalImagePath = normalImagePath;
        this.hoverImagePath = hoverImagePath;
        this.displayName = displayName;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.rotation = rotation;
        this.location = location;
        this.destinationPoint = destinationPoint;
        this.windowId = windowId;
        this.isInteractable = isInteractable;
    }

    public String getNormalImagePath() {
        return normalImagePath;
    }

    public String getHoverImagePath() {
        return hoverImagePath;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getRotation() {
        return rotation;
    }

    public PlayerState.Location getLocation() {
        return location;
    }

    public Point getDestinationPoint() {
        return destinationPoint;
    }

    public String getWindowId() {
        return windowId;
    }

    public boolean isInteractable() {
        return isInteractable;
    }

    public boolean hasHoverImage() {
        return hoverImagePath != null;
    }
}
