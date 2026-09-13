package fr.carrefour.reservation.infrastructure.adapter.in.web.dto;

import fr.carrefour.reservation.domain.model.Reservation;

import java.time.Instant;
import java.util.UUID;

import static fr.carrefour.reservation.infrastructure.adapter.in.web.dto.ReservationStatusResponse.valueOf;

public record ReservationResponse(
        UUID id,
        UUID eventId,
        String seatId,
        String customerId,
        ReservationStatusResponse status,
        Instant createdAt,
        Instant expiresAt
) {

    public static ReservationResponse from(
            Reservation reservation
    ) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getEventId(),
                reservation.getSeatId(),
                reservation.getCustomerId(),
                valueOf(
                        reservation.getStatus().name()
                ),
                reservation.getCreatedAt(),
                reservation.getExpiresAt()
        );
    }
}
