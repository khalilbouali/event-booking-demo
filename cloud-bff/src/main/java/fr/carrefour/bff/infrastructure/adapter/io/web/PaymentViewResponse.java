package fr.carrefour.bff.infrastructure.adapter.io.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentViewResponse(
        UUID id,
        UUID reservationId,
        BigDecimal amount,
        String status,
        Instant createdAt,

        UUID eventId,
        String seatId,
        String eventName,
        String eventVenue,
        Instant eventStartsAt
) {

    public static PaymentViewResponse from(
            PaymentResponse payment,
            ReservationResponse reservation,
            EventDetailsResponse event
    ) {
        return new PaymentViewResponse(
                payment.id(),
                payment.reservationId(),
                payment.amount(),
                payment.status(),
                payment.createdAt(),

                reservation != null
                        ? reservation.eventId()
                        : null,

                reservation != null
                        ? reservation.seatId()
                        : null,

                event != null
                        ? event.name()
                        : "Unknown event",

                event != null
                        ? event.venue()
                        : null,

                event != null
                        ? event.startsAt()
                        : null
        );
    }
}
