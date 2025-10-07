package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameObjectFactory {
    private static GameObjectFactory instance;
    private final Map<GameObjectType, GameObject> objectMap;
    private final Map<PlayerState.Location, GameObjectType> locationToTypeMap;
    private final List<GameObject> allObjects;

    private GameObjectFactory() {
        this.objectMap = new HashMap<>();
        this.locationToTypeMap = new HashMap<>();
        this.allObjects = new ArrayList<>();
        initializeObjects();
    }

    public static GameObjectFactory getInstance() {
        if (instance == null) {
            instance = new GameObjectFactory();
        }
        return instance;
    }

    private void initializeObjects() {
        for (GameObjectType type : GameObjectType.values()) {
            GameObject obj;

            if (type.hasHoverImage()) {
                obj = new GameObject(
                        type.getNormalImagePath(),
                        type.getHoverImagePath(),
                        type.getDisplayName(),
                        type.getX(),
                        type.getY(),
                        type.getWidth(),
                        type.getHeight(),
                        type.getRotation());
            } else {
                obj = new GameObject(
                        type.getNormalImagePath(),
                        type.getDisplayName(),
                        type.getX(),
                        type.getY(),
                        type.getWidth(),
                        type.getHeight(),
                        type.getRotation());
            }

            objectMap.put(type, obj);
            allObjects.add(obj);

            if (type.getLocation() != null) {
                locationToTypeMap.put(type.getLocation(), type);
            }
        }
    }

    public List<GameObject> getAllObjects() {
        return new ArrayList<>(allObjects);
    }

    public GameObject getObject(GameObjectType type) {
        return objectMap.get(type);
    }

    public GameObjectType getTypeByLocation(PlayerState.Location location) {
        return locationToTypeMap.get(location);
    }

    public String getWindowIdForLocation(PlayerState.Location location) {
        GameObjectType type = locationToTypeMap.get(location);
        return type != null ? type.getWindowId() : null;
    }

    public boolean isInteractable(GameObject obj) {
        for (Map.Entry<GameObjectType, GameObject> entry : objectMap.entrySet()) {
            if (entry.getValue().equals(obj)) {
                return entry.getKey().isInteractable();
            }
        }
        return false;
    }

    public PlayerState.Location getLocationForObject(GameObject obj) {
        for (Map.Entry<GameObjectType, GameObject> entry : objectMap.entrySet()) {
            if (entry.getValue().equals(obj)) {
                return entry.getKey().getLocation();
            }
        }
        return null;
    }

    public List<GameObject> getInteractableObjects() {
        List<GameObject> interactable = new ArrayList<>();
        for (GameObjectType type : GameObjectType.values()) {
            if (type.isInteractable()) {
                interactable.add(objectMap.get(type));
            }
        }
        return interactable;
    }

    public void reset() {
        objectMap.clear();
        locationToTypeMap.clear();
        allObjects.clear();
        initializeObjects();
    }
}
