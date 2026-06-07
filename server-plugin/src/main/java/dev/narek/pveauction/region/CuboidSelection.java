package dev.narek.pveauction.region;

import org.bukkit.Location;

/** Выделение как в WE: две точки топором + expand up/down. */
public final class CuboidSelection {

    private Location pos1;
    private Location pos2;
    private int minX;
    private int maxX;
    private int minY;
    private int maxY;
    private int minZ;
    private int maxZ;

    public Location pos1() {
        return pos1;
    }

    public Location pos2() {
        return pos2;
    }

    public boolean isComplete() {
        return pos1 != null && pos2 != null
                && pos1.getWorld() != null && pos2.getWorld() != null
                && pos1.getWorld().equals(pos2.getWorld());
    }

    public String worldName() {
        return pos1 != null && pos1.getWorld() != null ? pos1.getWorld().getName() : null;
    }

    public int minX() {
        return minX;
    }

    public int maxX() {
        return maxX;
    }

    public int minY() {
        return minY;
    }

    public int maxY() {
        return maxY;
    }

    public int minZ() {
        return minZ;
    }

    public int maxZ() {
        return maxZ;
    }

    public void setPos1(Location loc) {
        this.pos1 = loc.clone();
        refreshBounds();
    }

    public void setPos2(Location loc) {
        this.pos2 = loc.clone();
        refreshBounds();
    }

    /** Вверх от текущей верхней грани: maxY += amount */
    public void expandUp(int amount) {
        if (amount <= 0) {
            return;
        }
        maxY += amount;
    }

    /** Вниз от текущей нижней грани: minY -= amount */
    public void expandDown(int amount) {
        if (amount <= 0) {
            return;
        }
        minY -= amount;
    }

    private void refreshBounds() {
        if (!isComplete()) {
            return;
        }
        minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
    }

    public String boundsSummary() {
        return minX + ".." + maxX + ", Y " + minY + ".." + maxY + ", " + minZ + ".." + maxZ;
    }
}
