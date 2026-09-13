package fr.carrefour.bff.infrastructure.adapter.io.web;

import java.time.Instant;
import java.util.UUID;

public record ReservationViewResponse(
        UUID id,
        UUID eventId,
        String seatId,
        String status,
        Instant createdAt,
        Instant expiresAt,
        String eventName,
        String eventVenue,
        Instant eventStartsAt
) {

    public static ReservationViewResponse from(
            ReservationResponse reservation,
            EventDetailsResponse event
    ) {
        return new ReservationViewResponse(
                reservation.id(),
                reservation.eventId(),
                reservation.seatId(),
                reservation.status(),
                reservation.createdAt(),
                reservation.expiresAt(),
                event != null ? event.name() : "Unknown event",
                event != null ? event.venue() : null,
                event != null ? event.startsAt() : null
        );
    }
}
