package fr.carrefour.reservation.infrastructure.adapter.out.persistence.mongodb;

import fr.carrefour.reservation.application.port.out.ReservationRepository;
import fr.carrefour.reservation.domain.exception.SeatUnavailableException;
import fr.carrefour.reservation.domain.model.Reservation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static fr.carrefour.reservation.domain.model.ReservationStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest(
        properties = {
                "spring.mongodb.representation.uuid=standard",
                "spring.data.mongodb.auto-index-creation=true"
        }
)
@Import({
        MongoReservationRepositoryAdapter.class,
        ReservationPersistenceMapper.class
})
@Testcontainers
class MongoReservationRepositoryAdapterIT {

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-10T10:00:00Z"
            );

    @Container
    @ServiceConnection
    static MongoDBContainer mongo =
            new MongoDBContainer(
                    "mongo:8.0"
            );

    @Autowired
    private ReservationRepository repository;

    @Test
    void shouldSaveAndFindReservationById() {

        UUID reservationId =
                UUID.randomUUID();

        UUID eventId =
                UUID.randomUUID();

        Instant createdAt =
                NOW.minus(
                        Duration.ofMinutes(10)
                );

        Instant expiresAt =
                NOW.plus(
                        Duration.ofMinutes(10)
                );

        Reservation reservation =
                Reservation.restore(
                        reservationId,
                        eventId,
                        "A-01-01",
                        "customer-1",
                        HELD,
                        createdAt,
                        expiresAt
                );

        StepVerifier
                .create(
                        repository.save(reservation)
                                .then(
                                        repository.findById(
                                                reservationId
                                        )
                                )
                )
                .assertNext(saved -> {

                    assertThat(
                            saved.getId()
                    ).isEqualTo(
                            reservationId
                    );

                    assertThat(
                            saved.getEventId()
                    ).isEqualTo(
                            eventId
                    );

                    assertThat(
                            saved.getSeatId()
                    ).isEqualTo(
                            "A-01-01"
                    );

                    assertThat(
                            saved.getCustomerId()
                    ).isEqualTo(
                            "customer-1"
                    );

                    assertThat(
                            saved.getStatus()
                    ).isEqualTo(
                            HELD
                    );

                    assertThat(
                            saved.getCreatedAt()
                    ).isEqualTo(
                            createdAt
                    );

                    assertThat(
                            saved.getExpiresAt()
                    ).isEqualTo(
                            expiresAt
                    );
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenReservationDoesNotExist() {

        StepVerifier
                .create(
                        repository.findById(
                                UUID.randomUUID()
                        )
                )
                .verifyComplete();
    }

    @Test
    void shouldPreventTwoActiveReservationsForSameSeat() {

        UUID eventId =
                UUID.randomUUID();

        String seatId =
                "A-02-01";

        Reservation first =
                reservation(
                        UUID.randomUUID(),
                        eventId,
                        seatId,
                        "customer-1",
                        HELD,
                        NOW.plus(
                                Duration.ofMinutes(10)
                        )
                );

        Reservation second =
                reservation(
                        UUID.randomUUID(),
                        eventId,
                        seatId,
                        "customer-2",
                        HELD,
                        NOW.plus(
                                Duration.ofMinutes(10)
                        )
                );

        StepVerifier
                .create(
                        repository.save(first)
                                .then(
                                        repository.save(
                                                second
                                        )
                                )
                )
                .expectError(
                        SeatUnavailableException.class
                )
                .verify();
    }

    @Test
    void shouldTreatConfirmedReservationAsBlockingSeat() {

        UUID eventId =
                UUID.randomUUID();

        String seatId =
                "A-03-01";

        Reservation confirmed =
                reservation(
                        UUID.randomUUID(),
                        eventId,
                        seatId,
                        "customer-1",
                        CONFIRMED,
                        NOW.plus(
                                Duration.ofMinutes(10)
                        )
                );

        Reservation competing =
                reservation(
                        UUID.randomUUID(),
                        eventId,
                        seatId,
                        "customer-2",
                        HELD,
                        NOW.plus(
                                Duration.ofMinutes(10)
                        )
                );

        StepVerifier
                .create(
                        repository.save(confirmed)
                                .then(
                                        repository.save(
                                                competing
                                        )
                                )
                )
                .expectError(
                        SeatUnavailableException.class
                )
                .verify();
    }

    @Test
    void shouldAllowSeatToBeReservedAgainAfterCancellation() {

        UUID eventId =
                UUID.randomUUID();

        String seatId =
                "A-04-01";

        Reservation first =
                reservation(
                        UUID.randomUUID(),
                        eventId,
                        seatId,
                        "customer-1",
                        HELD,
                        NOW.plus(
                                Duration.ofMinutes(10)
                        )
                );

        StepVerifier
                .create(
                        repository.save(first)
                )
                .expectNextCount(1)
                .verifyComplete();

        first.cancel();

        StepVerifier
                .create(
                        repository.save(first)
                )
                .expectNextCount(1)
                .verifyComplete();

        Reservation second =
                reservation(
                        UUID.randomUUID(),
                        eventId,
                        seatId,
                        "customer-2",
                        HELD,
                        NOW.plus(
                                Duration.ofMinutes(10)
                        )
                );

        StepVerifier
                .create(
                        repository.save(second)
                )
                .assertNext(saved -> {

                    assertThat(
                            saved.getSeatId()
                    ).isEqualTo(
                            seatId
                    );

                    assertThat(
                            saved.getStatus()
                    ).isEqualTo(
                            HELD
                    );
                })
                .verifyComplete();
    }

    @Test
    void shouldAllowSeatToBeReservedAgainAfterExpiration() {

        UUID eventId =
                UUID.randomUUID();

        String seatId =
                "A-05-01";

        Reservation first =
                reservation(
                        UUID.randomUUID(),
                        eventId,
                        seatId,
                        "customer-1",
                        HELD,
                        NOW
                );

        StepVerifier
                .create(
                        repository.save(first)
                )
                .expectNextCount(1)
                .verifyComplete();

        first.expire(NOW);

        assertThat(
                first.getStatus()
        ).isEqualTo(
                EXPIRED
        );

        StepVerifier
                .create(
                        repository.save(first)
                )
                .expectNextCount(1)
                .verifyComplete();

        Reservation second =
                reservation(
                        UUID.randomUUID(),
                        eventId,
                        seatId,
                        "customer-2",
                        HELD,
                        NOW.plus(
                                Duration.ofMinutes(10)
                        )
                );

        StepVerifier
                .create(
                        repository.save(second)
                )
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldFindOnlyBlockedSeats() {

        UUID eventId =
                UUID.randomUUID();

        Reservation held =
                reservation(
                        UUID.randomUUID(),
                        eventId,
                        "B-01-01",
                        "customer-1",
                        HELD,
                        NOW.plus(
                                Duration.ofMinutes(10)
                        )
                );

        Reservation confirmed =
                reservation(
                        UUID.randomUUID(),
                        eventId,
                        "B-01-02",
                        "customer-2",
                        CONFIRMED,
                        NOW.plus(
                                Duration.ofMinutes(10)
                        )
                );

        Reservation cancelled =
                reservation(
                        UUID.randomUUID(),
                        eventId,
                        "B-01-03",
                        "customer-3",
                        CANCELLED,
                        NOW.plus(
                                Duration.ofMinutes(10)
                        )
                );

        Reservation expired =
                reservation(
                        UUID.randomUUID(),
                        eventId,
                        "B-01-04",
                        "customer-4",
                        EXPIRED,
                        NOW
                );

        StepVerifier
                .create(
                        repository.save(held)
                                .then(
                                        repository.save(
                                                confirmed
                                        )
                                )
                                .then(
                                        repository.save(
                                                cancelled
                                        )
                                )
                                .then(
                                        repository.save(
                                                expired
                                        )
                                )
                                .then(
                                        repository.findBlockedSeatIds(
                                                eventId,
                                                List.of(
                                                        "B-01-01",
                                                        "B-01-02",
                                                        "B-01-03",
                                                        "B-01-04"
                                                )
                                        )
                                )
                )
                .assertNext(blockedSeatIds ->
                        assertThat(
                                blockedSeatIds
                        )
                                .containsExactlyInAnyOrder(
                                        "B-01-01",
                                        "B-01-02"
                                )
                )
                .verifyComplete();
    }

    @Test
    void shouldFindExpiredHeldReservations() {

        UUID eventId =
                UUID.randomUUID();

        Reservation expiredHold =
                reservation(
                        UUID.randomUUID(),
                        eventId,
                        "C-01-01",
                        "customer-expired",
                        HELD,
                        NOW.minusSeconds(1)
                );

        Reservation futureHold =
                reservation(
                        UUID.randomUUID(),
                        eventId,
                        "C-01-02",
                        "customer-future",
                        HELD,
                        NOW.plusSeconds(60)
                );

        Reservation confirmed =
                reservation(
                        UUID.randomUUID(),
                        eventId,
                        "C-01-03",
                        "customer-confirmed",
                        CONFIRMED,
                        NOW.minusSeconds(1)
                );

        StepVerifier
                .create(
                        repository.save(expiredHold)
                                .then(
                                        repository.save(
                                                futureHold
                                        )
                                )
                                .then(
                                        repository.save(
                                                confirmed
                                        )
                                )
                                .thenMany(
                                        repository.findExpiredHolds(
                                                NOW
                                        )
                                )
                                .collectList()
                )
                .assertNext(reservations ->

                    assertThat(
                            reservations
                    )
                            .extracting(
                                    Reservation::getId
                            )
                            .contains(
                                    expiredHold.getId()
                            )
                            .doesNotContain(
                                    futureHold.getId(),
                                    confirmed.getId()
                            ))
                .verifyComplete();
    }

    @Test
    void shouldFindReservationsByCustomerId() {

        UUID eventId =
                UUID.randomUUID();

        String customerId =
                "customer-" + UUID.randomUUID();

        Reservation first =
                reservation(
                        UUID.randomUUID(),
                        eventId,
                        "D-01-01",
                        customerId,
                        HELD,
                        NOW.plusSeconds(60)
                );

        Reservation second =
                reservation(
                        UUID.randomUUID(),
                        eventId,
                        "D-01-02",
                        customerId,
                        CONFIRMED,
                        NOW.plusSeconds(60)
                );

        Reservation anotherCustomer =
                reservation(
                        UUID.randomUUID(),
                        eventId,
                        "D-01-03",
                        "another-" + UUID.randomUUID(),
                        HELD,
                        NOW.plusSeconds(60)
                );

        StepVerifier
                .create(
                        repository.save(first)
                                .then(
                                        repository.save(
                                                second
                                        )
                                )
                                .then(
                                        repository.save(
                                                anotherCustomer
                                        )
                                )
                                .thenMany(
                                        repository.findByCustomerId(
                                                customerId
                                        )
                                )
                                .collectList()
                )
                .assertNext(reservations -> {

                    assertThat(
                            reservations
                    )
                            .extracting(
                                    Reservation::getId
                            )
                            .containsExactlyInAnyOrder(
                                    first.getId(),
                                    second.getId()
                            );

                    assertThat(
                            reservations
                    )
                            .allMatch(
                                    reservation ->
                                            customerId.equals(
                                                    reservation.getCustomerId()
                                            )
                            );
                })
                .verifyComplete();
    }

    private static Reservation reservation(
            UUID reservationId,
            UUID eventId,
            String seatId,
            String customerId,
            fr.carrefour.reservation.domain.model.ReservationStatus status,
            Instant expiresAt
    ) {

        return Reservation.restore(
                reservationId,
                eventId,
                seatId,
                customerId,
                status,
                NOW.minus(
                        Duration.ofMinutes(10)
                ),
                expiresAt
        );
    }
}