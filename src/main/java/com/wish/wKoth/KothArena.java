package com.wish.wKoth;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class KothArena {
    private String id;
    private String name;
    private Location pos1;
    private Location pos2;
    private int duration;
    private boolean isActive;
    private Player currentKingPlayer;
    private int captureTime;

    public KothArena(String id, String name, int duration) {
        this.id = id;
        this.name = name;
        this.duration = duration;
        this.isActive = false;
        this.captureTime = 0;
    }

    // Getters y Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }
    public int getDuration() { return duration; }
    public boolean isActive() { return isActive; }
    public Player getCurrentKingPlayer() { return currentKingPlayer; }
    public int getCaptureTime() { return captureTime; }

    public void setPos1(Location pos1) { this.pos1 = pos1; }
    public void setPos2(Location pos2) { this.pos2 = pos2; }
    public void setActive(boolean active) { this.isActive = active; }
    public void setCurrentKingPlayer(Player player) { this.currentKingPlayer = player; }
    public void setCaptureTime(int time) { this.captureTime = time; }

    public boolean isConfigured() {
        return pos1 != null && pos2 != null;
    }

    public boolean isInside(Location loc) {
        if (pos1 == null || pos2 == null) return false;

        double minX = Math.min(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        return loc.getX() >= minX && loc.getX() <= maxX &&
                loc.getY() >= minY && loc.getY() <= maxY &&
                loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }
}