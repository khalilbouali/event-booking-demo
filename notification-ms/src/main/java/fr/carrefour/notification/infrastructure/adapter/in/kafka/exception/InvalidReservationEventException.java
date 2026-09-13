package fr.carrefour.notification.infrastructure.adapter.in.kafka.exception;

public class InvalidReservationEventException extends RuntimeException {

    public InvalidReservationEventException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
