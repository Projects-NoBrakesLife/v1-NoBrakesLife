package core;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class PathFinder {
    private final List<Point> roadPoints;

    public PathFinder(List<Point> roadPoints) {
        this.roadPoints = roadPoints;
    }

    public List<Point> findPath(Point from, Point to) {
        if (roadPoints.isEmpty()) {
            return calculateDirectPath(from, to);
        }
        return calculateRoadPath(from, to);
    }

    private List<Point> calculateDirectPath(Point from, Point to) {
        List<Point> path = new ArrayList<>();
        int dx = to.x - from.x;
        int dy = to.y - from.y;
        
        if (Math.abs(dx) > Math.abs(dy)) {
            path.add(new Point(from.x + dx/2, from.y));
            path.add(new Point(to.x, from.y));
        } else {
            path.add(new Point(from.x, from.y + dy/2));
            path.add(new Point(from.x, to.y));
        }
        path.add(to);
        return path;
    }

    private List<Point> calculateRoadPath(Point from, Point to) {
        List<Point> path = new ArrayList<>();
        Point nearestFrom = findNearestRoadPoint(from);
        Point nearestTo = findNearestRoadPoint(to);
        
        if (nearestFrom != null && nearestTo != null) {
            path.add(from);
            if (!from.equals(nearestFrom)) {
                path.add(nearestFrom);
            }
            
            List<Point> roadPath = findRoadPath(nearestFrom, nearestTo);
            path.addAll(roadPath);
            
            if (!nearestTo.equals(to)) {
                path.add(to);
            }
        } else {
            return calculateDirectPath(from, to);
        }
        return path;
    }

    private Point findNearestRoadPoint(Point target) {
        if (roadPoints.isEmpty()) return null;
        
        Point nearest = roadPoints.get(0);
        double minDistance = target.distance(nearest);
        
        for (Point roadPoint : roadPoints) {
            double distance = target.distance(roadPoint);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = roadPoint;
            }
        }
        return nearest;
    }

    private List<Point> findRoadPath(Point from, Point to) {
        List<Point> path = new ArrayList<>();
        int fromIndex = findRoadPointIndex(from);
        int toIndex = findRoadPointIndex(to);
        
        if (fromIndex == -1 || toIndex == -1) return path;
        
        int totalPoints = roadPoints.size();
        int forwardDistance = (toIndex - fromIndex + totalPoints) % totalPoints;
        int backwardDistance = (fromIndex - toIndex + totalPoints) % totalPoints;
        
        if (forwardDistance <= backwardDistance) {
            for (int i = fromIndex; i != toIndex; i = (i + 1) % totalPoints) {
                path.add(roadPoints.get(i));
            }
            path.add(roadPoints.get(toIndex));
        } else {
            for (int i = fromIndex; i != toIndex; i = (i - 1 + totalPoints) % totalPoints) {
                path.add(roadPoints.get(i));
            }
            path.add(roadPoints.get(toIndex));
        }
        return path;
    }
    
    private int findRoadPointIndex(Point target) {
        if (roadPoints.isEmpty()) return -1;
        
        int nearestIndex = 0;
        double minDistance = target.distance(roadPoints.get(0));
        
        for (int i = 1; i < roadPoints.size(); i++) {
            double distance = target.distance(roadPoints.get(i));
            if (distance < minDistance) {
                minDistance = distance;
                nearestIndex = i;
            }
        }
        return nearestIndex;
    }
}
