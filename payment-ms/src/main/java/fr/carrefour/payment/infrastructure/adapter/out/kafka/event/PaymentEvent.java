package fr.carrefour.payment.infrastructure.adapter.out.kafka.event;

import fr.carrefour.payment.domain.model.PaymentStatus;

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