package fr.carrefour.event.domain.exception;

public class InvalidSeatException extends RuntimeException {

    public InvalidSeatException(String message) {
        super(message);
    }
}