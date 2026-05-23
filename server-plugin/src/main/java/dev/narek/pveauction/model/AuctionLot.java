package dev.narek.pveauction.model;

import java.util.UUID;

public record AuctionLot(
        long id,
        UUID sellerUuid,
        String sellerName,
        byte[] itemBlob,
        long price,
        long createdAt
) {}
