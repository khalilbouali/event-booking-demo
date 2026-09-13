package fr.carrefour.reservation.infrastructure.adapter.in.kafka.exception;

public class InvalidPaymentEventException extends RuntimeException {

    public InvalidPaymentEventException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}