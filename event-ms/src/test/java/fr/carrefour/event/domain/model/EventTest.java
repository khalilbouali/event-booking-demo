package fr.carrefour.event.domain.model;

import fr.carrefour.event.domain.exception.DuplicateSeatException;
import fr.carrefour.event.domain.exception.InvalidEventException;
import fr.carrefour.event.domain.service.SeatLayoutGenerator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventTest {

    private static final String NAME =
            "Carrefour Tech Event";

    private static final String VENUE =
            "Casablanca";

    private static final Instant STARTS_AT =
            Instant.parse("2026-10-01T18:00:00Z");

    private static final BigDecimal PRICE =
            new BigDecimal("150.00");

    private final SeatLayoutGenerator seatLayoutGenerator =
            new SeatLayoutGenerator();

    @Test
    void shouldKeepGeneratedSeats() {

        List<Seat> seats =
                seatLayoutGenerator.generate(3);

        Event event =
                Event.create(
                        "Java Conference",
                        "Rabat",
                        Instant.parse(
                                "2026-11-15T09:00:00Z"
                        ),
                        seats,
                        new BigDecimal("100.00")
                );

        assertThat(event.getSeats())
                .extracting(Seat::id)
                .containsExactly(
                        "A-01-01",
                        "A-01-02",
                        "A-01-03"
                );
    }

    @Test
    void shouldCreateEvent() {

        List<Seat> seats = List.of(
                seat("A-01-01"),
                seat("A-01-02")
        );

        Event event = Event.create(
                NAME,
                VENUE,
                STARTS_AT,
                seats,
                PRICE
        );

        assertThat(event.getId())
                .isNotNull();

        assertThat(event.getName())
                .isEqualTo(NAME);

        assertThat(event.getVenue())
                .isEqualTo(VENUE);

        assertThat(event.getStartsAt())
                .isEqualTo(STARTS_AT);

        assertThat(event.getSeats())
                .containsExactlyElementsOf(seats);

        assertThat(event.getPrice())
                .isEqualByComparingTo(PRICE);
    }

    @Test
    void shouldRestoreEventWithExistingId() {

        UUID id = UUID.randomUUID();

        Event event = Event.restore(
                id,
                NAME,
                VENUE,
                STARTS_AT,
                List.of(seat("A-01-01")),
                PRICE
        );

        assertThat(event.getId())
                .isEqualTo(id);
    }

    @Test
    void shouldRejectNullIdWhenRestoringEvent() {

        assertThatThrownBy(
                () -> Event.restore(
                        null,
                        NAME,
                        VENUE,
                        STARTS_AT,
                        List.of(seat("A-01-01")),
                        PRICE
                )
        )
                .isInstanceOf(
                        InvalidEventException.class
                )
                .hasMessage(
                        "Event id must not be null"
                );
    }

    @Test
    void shouldRejectNullName() {

        assertThatThrownBy(
                () -> Event.create(
                        null,
                        VENUE,
                        STARTS_AT,
                        List.of(seat("A-01-01")),
                        PRICE
                )
        )
                .isInstanceOf(
                        InvalidEventException.class
                )
                .hasMessage(
                        "Event name must not be blank"
                );
    }

    @Test
    void shouldRejectBlankName() {

        assertThatThrownBy(
                () -> Event.create(
                        "   ",
                        VENUE,
                        STARTS_AT,
                        List.of(seat("A-01-01")),
                        PRICE
                )
        )
                .isInstanceOf(
                        InvalidEventException.class
                )
                .hasMessage(
                        "Event name must not be blank"
                );
    }

    @Test
    void shouldRejectNullVenue() {

        assertThatThrownBy(
                () -> Event.create(
                        NAME,
                        null,
                        STARTS_AT,
                        List.of(seat("A-01-01")),
                        PRICE
                )
        )
                .isInstanceOf(
                        InvalidEventException.class
                )
                .hasMessage(
                        "Venue must not be blank"
                );
    }

    @Test
    void shouldRejectBlankVenue() {

        assertThatThrownBy(
                () -> Event.create(
                        NAME,
                        "   ",
                        STARTS_AT,
                        List.of(seat("A-01-01")),
                        PRICE
                )
        )
                .isInstanceOf(
                        InvalidEventException.class
                )
                .hasMessage(
                        "Venue must not be blank"
                );
    }

    @Test
    void shouldRejectNullStartDate() {

        assertThatThrownBy(
                () -> Event.create(
                        NAME,
                        VENUE,
                        null,
                        List.of(seat("A-01-01")),
                        PRICE
                )
        )
                .isInstanceOf(
                        InvalidEventException.class
                )
                .hasMessage(
                        "Event start date must not be null"
                );
    }

    @Test
    void shouldRejectNullSeats() {

        assertThatThrownBy(
                () -> Event.create(
                        NAME,
                        VENUE,
                        STARTS_AT,
                        null,
                        PRICE
                )
        )
                .isInstanceOf(
                        InvalidEventException.class
                )
                .hasMessage(
                        "Seats must not be null"
                );
    }

    @Test
    void shouldRejectEmptySeatList() {

        assertThatThrownBy(
                () -> Event.create(
                        NAME,
                        VENUE,
                        STARTS_AT,
                        List.of(),
                        PRICE
                )
        )
                .isInstanceOf(
                        InvalidEventException.class
                )
                .hasMessage(
                        "Event must contain at least one seat"
                );
    }

    @Test
    void shouldRejectNullSeat() {

        List<Seat> seats =
                new ArrayList<>();

        seats.add(seat("A-01-01"));
        seats.add(null);

        assertThatThrownBy(
                () -> Event.create(
                        NAME,
                        VENUE,
                        STARTS_AT,
                        seats,
                        PRICE
                )
        )
                .isInstanceOf(
                        InvalidEventException.class
                )
                .hasMessage(
                        "Seats must not contain null values"
                );
    }

    @Test
    void shouldRejectDuplicateSeatIds() {

        List<Seat> seats = List.of(
                seat("A-01-01"),
                seat("A-01-01")
        );

        assertThatThrownBy(
                () -> Event.create(
                        NAME,
                        VENUE,
                        STARTS_AT,
                        seats,
                        PRICE
                )
        )
                .isInstanceOf(
                        DuplicateSeatException.class
                );
    }

    @Test
    void shouldProtectSeatListFromExternalModification() {

        List<Seat> seats =
                new ArrayList<>();

        seats.add(
                seat("A-01-01")
        );

        Event event = Event.create(
                NAME,
                VENUE,
                STARTS_AT,
                seats,
                PRICE
        );

        seats.add(
                seat("A-01-02")
        );

        assertThat(event.getSeats())
                .hasSize(1)
                .extracting(Seat::id)
                .containsExactly(
                        "A-01-01"
                );
    }

    @Test
    void shouldExposeImmutableSeatList() {

        Event event = Event.create(
                NAME,
                VENUE,
                STARTS_AT,
                List.of(
                        seat("A-01-01")
                ),
                PRICE
        );

        assertThatThrownBy(
                () -> event
                        .getSeats()
                        .add(
                                seat("A-01-02")
                        )
        )
                .isInstanceOf(
                        UnsupportedOperationException.class
                );
    }

    @Test
    void shouldRejectNullPrice() {

        assertThatThrownBy(
                () -> Event.create(
                        NAME,
                        VENUE,
                        STARTS_AT,
                        List.of(seat("A-01-01")),
                        null
                )
        )
                .isInstanceOf(
                        InvalidEventException.class
                )
                .hasMessage(
                        "Event price must not be null"
                );
    }

    @Test
    void shouldRejectNegativePrice() {

        assertThatThrownBy(
                () -> Event.create(
                        NAME,
                        VENUE,
                        STARTS_AT,
                        List.of(seat("A-01-01")),
                        new BigDecimal("-0.01")
                )
        )
                .isInstanceOf(
                        InvalidEventException.class
                )
                .hasMessage(
                        "Event price must not be negative"
                );
    }

    @Test
    void shouldAllowZeroPrice() {

        Event event = Event.create(
                NAME,
                VENUE,
                STARTS_AT,
                List.of(seat("A-01-01")),
                BigDecimal.ZERO
        );

        assertThat(event.getPrice())
                .isEqualByComparingTo(
                        BigDecimal.ZERO
                );
    }

    private static Seat seat(
            String id
    ) {
        String[] parts =
                id.split("-");

        return new Seat(
                id,
                parts[0],
                parts[1],
                parts[2]
        );
    }
}