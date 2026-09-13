package fr.carrefour.bff.infrastructure.adapter.io.web;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID eventId,
        String seatId,
        String customerId,
        String status,
        Instant createdAt,
        Instant expiresAt
) {
}
