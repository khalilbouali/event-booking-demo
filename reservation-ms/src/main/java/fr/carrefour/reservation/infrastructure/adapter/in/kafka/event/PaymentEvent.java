package fr.carrefour.reservation.infrastructure.adapter.in.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record PaymentEvent(
        UUID messageId,
        UUID paymentId,
        UUID reservationId,
        PaymentStatus status,
        Instant occurredAt
) {
}