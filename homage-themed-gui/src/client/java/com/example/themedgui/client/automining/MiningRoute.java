package com.example.themedgui.client.automining;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a mining route with coordinates and target angles.
 */
public class MiningRoute {

    public static class RoutePoint {
        public final double x, y, z;
        public final float pitch, yaw;
        public final boolean useAOTV; // true = AOTV teleport, false = normal movement

        public RoutePoint(double x, double y, double z, float pitch, float yaw, boolean useAOTV) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.pitch = pitch;
            this.yaw = yaw;
            this.useAOTV = useAOTV;
        }
    }

    private final String name;
    private final List<RoutePoint> points;
    private int currentIndex = 0;
    private boolean loop = true;

    public MiningRoute(String name) {
        this.name = name;
        this.points = new ArrayList<>();
    }

    public void addPoint(double x, double y, double z, float pitch, float yaw, boolean useAOTV) {
        points.add(new RoutePoint(x, y, z, pitch, yaw, useAOTV));
    }

    public RoutePoint getCurrentPoint() {
        if (points.isEmpty()) return null;
        return points.get(currentIndex);
    }

    public RoutePoint getNextPoint() {
        if (points.isEmpty()) return null;

        currentIndex++;
        if (currentIndex >= points.size()) {
            if (loop) {
                currentIndex = 0;
            } else {
                currentIndex = points.size() - 1;
                return null; // End of route
            }
        }
        return points.get(currentIndex);
    }

    public void reset() {
        currentIndex = 0;
    }

    public boolean isComplete() {
        return !loop && currentIndex >= points.size() - 1;
    }

    public List<RoutePoint> getPoints() {
        return new ArrayList<>(points);
    }

    public String getName() {
        return name;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public boolean isLooping() {
        return loop;
    }
}