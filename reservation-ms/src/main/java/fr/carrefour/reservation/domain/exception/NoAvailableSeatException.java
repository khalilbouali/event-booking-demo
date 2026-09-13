package fr.carrefour.reservation.domain.exception;

import java.util.UUID;

public class NoAvailableSeatException extends RuntimeException {

    public NoAvailableSeatException(UUID eventId) {
        super(
                "No available seat for event " + eventId
        );
    }
}
