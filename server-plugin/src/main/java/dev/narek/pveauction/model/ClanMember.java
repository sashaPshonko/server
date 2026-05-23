package dev.narek.pveauction.model;

import java.util.UUID;

public record ClanMember(
        int clanId,
        UUID playerUuid,
        String playerName,
        ClanRole role,
        int permissions
) {
    public boolean isOwner() {
        return role == ClanRole.OWNER;
    }

    public boolean can(int flag) {
        return isOwner() || ClanPermissions.has(permissions, flag);
    }
}
