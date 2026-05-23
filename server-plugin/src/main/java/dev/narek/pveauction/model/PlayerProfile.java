package dev.narek.pveauction.model;

import java.util.UUID;

public record PlayerProfile(
        UUID uuid,
        String lastName,
        String rankId,
        String rankDisplayName,
        long tokens,
        Integer clanId,
        String clanName
) {
    public String clanDisplay() {
        if (clanName == null || clanName.isBlank()) {
            return "не состоит в клане";
        }
        return clanName;
    }
}
