package fr.carrefour.reservation.domain.exception;

import java.util.UUID;

public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(UUID id) {
        super("Reservation %s was not found".formatted(id));
    }
}