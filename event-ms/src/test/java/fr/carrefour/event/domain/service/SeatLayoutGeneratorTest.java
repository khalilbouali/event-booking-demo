package fr.carrefour.event.domain.service;

import fr.carrefour.event.domain.model.Seat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeatLayoutGeneratorTest {

    private SeatLayoutGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new SeatLayoutGenerator();
    }

    @Test
    void shouldGenerateRequestedNumberOfSeats() {

        List<Seat> seats = generator.generate(25);

        assertThat(seats)
                .hasSize(25);
    }

    @Test
    void shouldGenerateSeatWithExpectedProperties() {

        List<Seat> seats = generator.generate(1);

        Seat seat = seats.getFirst();

        assertThat(seat.id())
                .isEqualTo("A-01-01");

        assertThat(seat.section())
                .isEqualTo("A");

        assertThat(seat.row())
                .isEqualTo("01");

        assertThat(seat.number())
                .isEqualTo("01");
    }

    @Test
    void shouldGenerateSeatsInExpectedOrder() {

        List<Seat> seats = generator.generate(3);

        assertThat(seats)
                .extracting(Seat::id)
                .containsExactly(
                        "A-01-01",
                        "A-01-02",
                        "A-01-03"
                );
    }

    @Test
    void shouldMoveToNextRowAfterTenSeats() {

        List<Seat> seats = generator.generate(11);

        assertThat(seats.get(9).id())
                .isEqualTo("A-01-10");

        assertThat(seats.get(10).id())
                .isEqualTo("A-02-01");
    }

    @Test
    void shouldMoveToNextSectionAfterNinetyNineRows() {

        List<Seat> seats = generator.generate(991);

        assertThat(seats.get(989).id())
                .isEqualTo("A-99-10");

        assertThat(seats.get(990).id())
                .isEqualTo("B-01-01");
    }

    @Test
    void shouldGenerateUniqueSeatIds() {

        List<Seat> seats = generator.generate(
                SeatLayoutGenerator.MAX_SEATS
        );

        assertThat(seats)
                .extracting(Seat::id)
                .doesNotHaveDuplicates();
    }

    @Test
    void shouldGenerateMaximumSupportedLayout() {

        List<Seat> seats = generator.generate(
                SeatLayoutGenerator.MAX_SEATS
        );

        assertThat(seats)
                .hasSize(25_740);

        assertThat(seats.getFirst().id())
                .isEqualTo("A-01-01");

        assertThat(seats.getLast().id())
                .isEqualTo("Z-99-10");
    }

    @Test
    void shouldRejectZeroSeats() {

        assertThatThrownBy(
                () -> generator.generate(0)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Seat count must be between 1 and 25740"
                );
    }

    @Test
    void shouldRejectNegativeSeatCount() {

        assertThatThrownBy(
                () -> generator.generate(-1)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Seat count must be between 1 and 25740"
                );
    }

    @Test
    void shouldRejectSeatCountAboveMaximum() {

        assertThatThrownBy(
                () -> generator.generate(
                        SeatLayoutGenerator.MAX_SEATS + 1
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Seat count must be between 1 and 25740"
                );
    }
}