package dev.narek.pveauction.model;

import org.bukkit.Location;

/** Админ-зона: параллелепипед (X, Y, Z). */
public record AdminRegion(
        int id,
        String name,
        String world,
        int minX,
        int maxX,
        int minY,
        int maxY,
        int minZ,
        int maxZ,
        java.util.UUID createdByUuid,
        String createdByName,
        long createdAt,
        /** ПКМ по функциональным блокам для игроков без apriv */
        boolean allowMemberInteract,
        String homeWorld,
        Double homeX,
        Double homeY,
        Double homeZ,
        Float homeYaw,
        Float homePitch
) {
    public boolean contains(Location loc) {
        if (loc.getWorld() == null || !world.equalsIgnoreCase(loc.getWorld().getName())) {
            return false;
        }
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public boolean overlaps(AdminRegion other) {
        if (!world.equalsIgnoreCase(other.world)) {
            return false;
        }
        return minX <= other.maxX && maxX >= other.minX
                && minY <= other.maxY && maxY >= other.minY
                && minZ <= other.maxZ && maxZ >= other.minZ;
    }

    public long horizontalArea() {
        return (long) (maxX - minX + 1) * (maxZ - minZ + 1);
    }

    public long volume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public boolean hasHome() {
        return homeWorld != null && !homeWorld.isBlank()
                && homeX != null && homeY != null && homeZ != null;
    }

    public SavedLocation homeLocation() {
        if (!hasHome()) {
            return null;
        }
        float yaw = homeYaw != null ? homeYaw : 0f;
        float pitch = homePitch != null ? homePitch : 0f;
        return new SavedLocation(homeWorld, homeX, homeY, homeZ, yaw, pitch);
    }
}
