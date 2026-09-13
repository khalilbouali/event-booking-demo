package fr.carrefour.reservation.domain.exception;

public class SeatUnavailableException extends RuntimeException {

    public SeatUnavailableException(String seatId) {
        super("Seat %s is not available".formatted(seatId));
    }
}