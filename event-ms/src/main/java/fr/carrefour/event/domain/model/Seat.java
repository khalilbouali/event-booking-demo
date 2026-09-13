package fr.carrefour.event.domain.model;

import fr.carrefour.event.domain.exception.InvalidSeatException;

public record Seat(
        String id,
        String section,
        String row,
        String number
) {

    public Seat {
        if (id == null || id.isBlank()) {
            throw new InvalidSeatException(
                    "Seat id must not be blank"
            );
        }
    }
}
