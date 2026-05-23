package dev.narek.pveauction.model;

import java.util.UUID;

public record PlayerProfile(
        UUID uuid,
        String lastName,
        String rankId,
        String rankDisplayName,
        String rankColor,
        long tokens,
        Integer clanId,
        String clanName
) {
    public String clanDisplay() {
        if (clanName == null || clanName.isBlank()) {
            return "нет";
        }
        return clanName;
    }
}
