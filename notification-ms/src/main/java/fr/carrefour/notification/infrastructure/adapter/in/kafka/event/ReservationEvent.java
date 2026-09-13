package fr.carrefour.notification.infrastructure.adapter.in.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record ReservationEvent(
        UUID messageId,
        UUID reservationId,
        UUID eventId,
        String seatId,
        String customerId,
        ReservationStatus status,
        Instant occurredAt
) {
}
