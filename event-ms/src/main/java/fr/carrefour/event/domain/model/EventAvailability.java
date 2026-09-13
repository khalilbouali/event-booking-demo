package fr.carrefour.event.domain.model;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record EventAvailability(
        UUID eventId,
        int totalSeats,
        int remainingSeats
) {

    public EventAvailability {

        requireNonNull(
                eventId,
                "eventId must not be null"
        );

        if (totalSeats < 0) {
            throw new IllegalArgumentException(
                    "totalSeats must not be negative"
            );
        }

        if (remainingSeats < 0 ||
                remainingSeats > totalSeats) {

            throw new IllegalArgumentException(
                    "remainingSeats must be between 0 and totalSeats"
            );
        }
    }
}
