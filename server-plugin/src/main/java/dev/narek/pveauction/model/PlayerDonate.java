package dev.narek.pveauction.model;

public record PlayerDonate(
        String donateId,
        String displayName,
        String color,
        int sortOrder,
        Long expiresAt,
        long grantedAt
) {
    public boolean permanent() {
        return expiresAt == null;
    }
}
