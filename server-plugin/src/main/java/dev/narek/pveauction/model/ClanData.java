package dev.narek.pveauction.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public record ClanData(
        int id,
        String name,
        UUID ownerUuid,
        long balance,
        String homeWorld,
        Double homeX,
        Double homeY,
        Double homeZ,
        Float homeYaw,
        Float homePitch
) {
    public boolean hasHome() {
        return homeWorld != null && homeX != null && homeY != null && homeZ != null;
    }

    public Location homeLocation() {
        if (!hasHome()) {
            return null;
        }
        World world = Bukkit.getWorld(homeWorld);
        if (world == null) {
            return null;
        }
        float yaw = homeYaw == null ? 0f : homeYaw;
        float pitch = homePitch == null ? 0f : homePitch;
        return new Location(world, homeX, homeY, homeZ, yaw, pitch);
    }
}
