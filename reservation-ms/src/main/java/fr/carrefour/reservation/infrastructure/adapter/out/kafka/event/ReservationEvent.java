package fr.carrefour.reservation.infrastructure.adapter.out.kafka.event;

import fr.carrefour.reservation.domain.model.ReservationStatus;

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