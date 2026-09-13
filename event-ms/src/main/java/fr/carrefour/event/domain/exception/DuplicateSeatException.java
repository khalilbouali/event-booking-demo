package fr.carrefour.event.domain.exception;

public class DuplicateSeatException extends RuntimeException {

    public DuplicateSeatException(String seatId) {
        super("Duplicate seat id in event: " + seatId);
    }
}