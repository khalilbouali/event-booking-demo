package fr.carrefour.event.domain.model;

import fr.carrefour.event.domain.exception.DuplicateSeatException;
import fr.carrefour.event.domain.exception.InvalidEventException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.List.copyOf;
import static java.util.UUID.randomUUID;

@Getter
public class Event {

    private final UUID id;
    private final String name;
    private final String venue;
    private final Instant startsAt;
    private final List<Seat> seats;
    private final BigDecimal price;

    private Event(
            UUID id,
            String name,
            String venue,
            Instant startsAt,
            List<Seat> seats,
            BigDecimal price
    ) {
        this.id = requireId(id);
        this.name = requireText(name, "Event name");
        this.venue = requireText(venue, "Venue");
        this.startsAt = requireStartsAt(startsAt);
        this.seats = requireSeats(seats);
        this.price = requirePrice(price);
    }

    public static Event create(
            String name,
            String venue,
            Instant startsAt,
            List<Seat> seats,
            BigDecimal price
    ) {
        return new Event(
                randomUUID(),
                name,
                venue,
                startsAt,
                seats,
                price
        );
    }

    public static Event restore(
            UUID id,
            String name,
            String venue,
            Instant startsAt,
            List<Seat> seats,
            BigDecimal price
    ) {
        return new Event(
                id,
                name,
                venue,
                startsAt,
                seats,
                price
        );
    }

    private static UUID requireId(UUID id) {
        if (id == null) {
            throw new InvalidEventException(
                    "Event id must not be null"
            );
        }

        return id;
    }

    private static String requireText(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new InvalidEventException(
                    field + " must not be blank"
            );
        }

        return value;
    }

    private static Instant requireStartsAt(Instant startsAt) {
        if (startsAt == null) {
            throw new InvalidEventException(
                    "Event start date must not be null"
            );
        }

        return startsAt;
    }

    private static List<Seat> requireSeats(List<Seat> seats) {
        if (seats == null) {
            throw new InvalidEventException(
                    "Seats must not be null"
            );
        }

        if (seats.isEmpty()) {
            throw new InvalidEventException(
                    "Event must contain at least one seat"
            );
        }

        Set<String> seatIds = new HashSet<>();

        for (Seat seat : seats) {
            if (seat == null) {
                throw new InvalidEventException(
                        "Seats must not contain null values"
                );
            }

            if (!seatIds.add(seat.id())) {
                throw new DuplicateSeatException(
                        seat.id()
                );
            }
        }

        return copyOf(seats);
    }

    private static BigDecimal requirePrice(BigDecimal price) {
        if (price == null) {
            throw new InvalidEventException(
                    "Event price must not be null"
            );
        }

        if (price.signum() < 0) {
            throw new InvalidEventException(
                    "Event price must not be negative"
            );
        }

        return price;
    }
}
