package fr.carrefour.reservation.domain.model;

import fr.carrefour.reservation.domain.exception.InvalidReservationStateException;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static fr.carrefour.reservation.domain.model.ReservationStatus.*;
import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;

@Getter
public class Reservation {

    private final UUID id;
    private final UUID eventId;
    private final String seatId;
    private final String customerId;
    private final Instant createdAt;
    private final Instant expiresAt;

    private ReservationStatus status;

    private Reservation(
            UUID id,
            UUID eventId,
            String seatId,
            String customerId,
            ReservationStatus status,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.id = requireNonNull(id, "Reservation id is required");
        this.eventId = requireNonNull(eventId, "Event id is required");
        this.seatId = requireNonBlank(seatId, "Seat id is required");
        this.customerId = requireNonBlank(customerId, "Customer id is required");
        this.status = requireNonNull(status, "Reservation status is required");
        this.createdAt = requireNonNull(createdAt, "Creation date is required");
        this.expiresAt = requireNonNull(expiresAt, "Expiration date is required");

        if (expiresAt.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                    "Expiration date cannot be before creation date"
            );
        }
    }

    public static Reservation hold(
            UUID eventId,
            String seatId,
            String customerId,
            Instant now,
            Duration holdDuration
    ) {
        requireNonNull(now, "Current time is required");
        requireNonNull(holdDuration, "Hold duration is required");

        if (holdDuration.isZero() || holdDuration.isNegative()) {
            throw new IllegalArgumentException(
                    "Hold duration must be greater than zero"
            );
        }

        return new Reservation(
                randomUUID(),
                eventId,
                seatId,
                customerId,
                HELD,
                now,
                now.plus(holdDuration)
        );
    }

    public static Reservation restore(
            UUID id,
            UUID eventId,
            String seatId,
            String customerId,
            ReservationStatus status,
            Instant createdAt,
            Instant expiresAt
    ) {
        return new Reservation(
                id,
                eventId,
                seatId,
                customerId,
                status,
                createdAt,
                expiresAt
        );
    }

    public void confirm(Instant now) {
        requireNonNull(now, "Current time is required");

        if (status == CONFIRMED) {
            return;
        }

        if (isExpiredAt(now)) {
            throw new InvalidReservationStateException(
                    "Expired reservation cannot be confirmed"
            );
        }

        requireHeld();

        status = CONFIRMED;
    }

    public void expire(Instant now) {
        requireNonNull(now, "Current time is required");

        if (isExpiredAt(now)) {
            status = EXPIRED;
        }
    }

    public void cancel() {
        if (status == CANCELLED) {
            return;
        }

        requireHeld();
        status = CANCELLED;
    }

    public boolean isExpiredAt(Instant now) {
        requireNonNull(now, "Current time is required");

        return status == HELD
                && !expiresAt.isAfter(now);
    }

    public boolean blocksSeatAvailability() {
        return status.blocksSeatAvailability();
    }

    private void requireHeld() {
        if (status != HELD) {
            throw new InvalidReservationStateException(
                    "Operation is only allowed for held reservations"
            );
        }
    }

    private static String requireNonBlank(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }
}