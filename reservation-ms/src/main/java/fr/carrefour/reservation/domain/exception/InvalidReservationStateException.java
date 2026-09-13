package fr.carrefour.reservation.domain.exception;

public class InvalidReservationStateException
        extends RuntimeException {

    public InvalidReservationStateException(String message) {
        super(message);
    }
}