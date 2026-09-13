package fr.carrefour.event.infrastructure.adapter.in.kafka.dto;

import java.time.Instant;
import java.util.UUID;

public record ReservationEventMessage(

        UUID messageId,

        UUID reservationId,

        UUID eventId,

        String seatId,

        String customerId,

        String status,

        Instant occurredAt
) {
}
