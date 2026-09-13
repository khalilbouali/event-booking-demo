package fr.carrefour.event.domain.service;

import fr.carrefour.event.domain.model.Seat;

import java.util.List;
import java.util.stream.IntStream;

public final class SeatLayoutGenerator {

    private static final int SEATS_PER_ROW = 10;
    private static final int ROWS_PER_SECTION = 99;
    private static final int SECTION_COUNT = 26;

    public static final int MAX_SEATS =
            SEATS_PER_ROW
                    * ROWS_PER_SECTION
                    * SECTION_COUNT;

    public List<Seat> generate(
            int seatCount
    ) {

        if (seatCount < 1 || seatCount > MAX_SEATS) {
            throw new IllegalArgumentException(
                    "Seat count must be between 1 and " + MAX_SEATS
            );
        }

        int seatsPerSection =
                SEATS_PER_ROW * ROWS_PER_SECTION;

        return IntStream
                .range(0, seatCount)
                .mapToObj(index -> {

                    int sectionIndex =
                            index / seatsPerSection;

                    int positionInSection =
                            index % seatsPerSection;

                    int rowNumber =
                            positionInSection / SEATS_PER_ROW + 1;

                    int seatNumber =
                            positionInSection % SEATS_PER_ROW + 1;

                    String section =
                            String.valueOf(
                                    (char) ('A' + sectionIndex)
                            );

                    String row =
                            "%02d".formatted(rowNumber);

                    String number =
                            "%02d".formatted(seatNumber);

                    String id =
                            "%s-%s-%s".formatted(
                                    section,
                                    row,
                                    number
                            );

                    return new Seat(
                            id,
                            section,
                            row,
                            number
                    );
                })
                .toList();
    }
}
