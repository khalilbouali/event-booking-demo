package fr.carrefour.reservation.domain.model;

public enum ReservationStatus {
    HELD,
    CONFIRMED,
    EXPIRED,
    CANCELLED;

    public boolean blocksSeatAvailability() {
        return this == HELD || this == CONFIRMED;
    }
}