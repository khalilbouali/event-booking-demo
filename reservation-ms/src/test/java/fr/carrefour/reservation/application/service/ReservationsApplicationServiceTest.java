package fr.carrefour.reservation.application.service;

import fr.carrefour.reservation.application.model.EventSeatCandidates;
import fr.carrefour.reservation.application.port.out.EventSeatCatalog;
import fr.carrefour.reservation.application.port.out.ReservationEventPublisher;
import fr.carrefour.reservation.application.port.out.ReservationRepository;
import fr.carrefour.reservation.domain.exception.NoAvailableSeatException;
import fr.carrefour.reservation.domain.exception.ReservationNotFoundException;
import fr.carrefour.reservation.domain.exception.SeatUnavailableException;
import fr.carrefour.reservation.domain.model.Reservation;
import fr.carrefour.reservation.domain.model.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static fr.carrefour.reservation.domain.model.ReservationStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationsApplicationServiceTest {

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "11111111-1111-1111-1111-111111111111"
            );

    private static final UUID RESERVATION_ID =
            UUID.fromString(
                    "22222222-2222-2222-2222-222222222222"
            );

    private static final String CUSTOMER_ID =
            "customer-123";

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-10T10:00:00Z"
            );

    private static final Duration HOLD_DURATION =
            Duration.ofMinutes(10);

    private static final Clock CLOCK =
            Clock.fixed(
                    NOW,
                    ZoneOffset.UTC
            );

    @Mock
    private ReservationRepository repository;

    @Mock
    private ReservationEventPublisher eventPublisher;

    @Mock
    private EventSeatCatalog eventSeatCatalog;

    private ReservationsApplicationService service;

    @BeforeEach
    void setUp() {

        service =
                new ReservationsApplicationService(
                        repository,
                        eventPublisher,
                        eventSeatCatalog,
                        CLOCK,
                        HOLD_DURATION
                );
    }

    @Test
    void shouldHoldFirstAvailableSeat() {

        EventSeatCandidates candidates =
                new EventSeatCandidates(
                        EVENT_ID,
                        List.of(
                                "A-01-01",
                                "A-01-02",
                                "A-01-03"
                        )
                );

        when(
                eventSeatCatalog.getSeatCandidates(
                        EVENT_ID,
                        20
                )
        ).thenReturn(
                Mono.just(candidates)
        );

        when(
                repository.findBlockedSeatIds(
                        EVENT_ID,
                        candidates.seatIds()
                )
        ).thenReturn(
                Mono.just(
                        Set.of(
                                "A-01-01"
                        )
                )
        );

        when(
                repository.save(
                        any(Reservation.class)
                )
        ).thenAnswer(
                invocation ->
                        Mono.just(
                                invocation.getArgument(0)
                        )
        );

        when(
                eventPublisher.publish(
                        any(Reservation.class)
                )
        ).thenReturn(
                Mono.empty()
        );

        StepVerifier
                .create(
                        service.hold(
                                EVENT_ID,
                                CUSTOMER_ID
                        )
                )
                .assertNext(reservation -> {

                    assertThat(
                            reservation.getEventId()
                    ).isEqualTo(EVENT_ID);

                    assertThat(
                            reservation.getSeatId()
                    ).isEqualTo(
                            "A-01-02"
                    );

                    assertThat(
                            reservation.getCustomerId()
                    ).isEqualTo(
                            CUSTOMER_ID
                    );

                    assertThat(
                            reservation.getStatus()
                    ).isEqualTo(HELD);

                    assertThat(
                            reservation.getCreatedAt()
                    ).isEqualTo(NOW);

                    assertThat(
                            reservation.getExpiresAt()
                    ).isEqualTo(
                            NOW.plus(
                                    HOLD_DURATION
                            )
                    );
                })
                .verifyComplete();

        verify(
                eventSeatCatalog
        ).getSeatCandidates(
                EVENT_ID,
                20
        );

        verify(
                repository
        ).findBlockedSeatIds(
                EVENT_ID,
                candidates.seatIds()
        );

        verify(
                repository,
                times(1)
        ).save(
                any(Reservation.class)
        );

        verify(
                eventPublisher,
                times(1)
        ).publish(
                any(Reservation.class)
        );
    }

    @Test
    void shouldTryNextCandidateWhenFirstSeatBecomesUnavailable() {

        EventSeatCandidates candidates =
                new EventSeatCandidates(
                        EVENT_ID,
                        List.of(
                                "A-01-01",
                                "A-01-02"
                        )
                );

        when(
                eventSeatCatalog.getSeatCandidates(
                        EVENT_ID,
                        20
                )
        ).thenReturn(
                Mono.just(candidates)
        );

        when(
                repository.findBlockedSeatIds(
                        EVENT_ID,
                        candidates.seatIds()
                )
        ).thenReturn(
                Mono.just(
                        Set.of()
                )
        );

        when(
                repository.save(
                        any(Reservation.class)
                )
        ).thenAnswer(invocation -> {

            Reservation reservation =
                    invocation.getArgument(0);

            if (
                    reservation
                            .getSeatId()
                            .equals(
                                    "A-01-01"
                            )
            ) {
                return Mono.error(
                        new SeatUnavailableException(
                                "A-01-01"
                        )
                );
            }

            return Mono.just(
                    reservation
            );
        });

        when(
                eventPublisher.publish(
                        any(Reservation.class)
                )
        ).thenReturn(
                Mono.empty()
        );

        StepVerifier
                .create(
                        service.hold(
                                EVENT_ID,
                                CUSTOMER_ID
                        )
                )
                .assertNext(reservation ->
                        assertThat(
                                reservation.getSeatId()
                        ).isEqualTo(
                                "A-01-02"
                        )
                )
                .verifyComplete();

        verify(
                repository,
                times(2)
        ).save(
                any(Reservation.class)
        );

        verify(
                eventPublisher,
                times(1)
        ).publish(
                any(Reservation.class)
        );
    }

    @Test
    void shouldReturnNoAvailableSeatWhenCandidateListIsEmpty() {

        EventSeatCandidates candidates =
                new EventSeatCandidates(
                        EVENT_ID,
                        List.of()
                );

        when(
                eventSeatCatalog.getSeatCandidates(
                        EVENT_ID,
                        20
                )
        ).thenReturn(
                Mono.just(candidates)
        );

        StepVerifier
                .create(
                        service.hold(
                                EVENT_ID,
                                CUSTOMER_ID
                        )
                )
                .expectError(
                        NoAvailableSeatException.class
                )
                .verify();

        verifyNoInteractions(
                repository
        );

        verifyNoInteractions(
                eventPublisher
        );
    }

    @Test
    void shouldReturnNoAvailableSeatWhenAllCandidatesAreAlreadyBlocked() {

        EventSeatCandidates candidates =
                new EventSeatCandidates(
                        EVENT_ID,
                        List.of(
                                "A-01-01",
                                "A-01-02"
                        )
                );

        when(
                eventSeatCatalog.getSeatCandidates(
                        EVENT_ID,
                        20
                )
        ).thenReturn(
                Mono.just(candidates)
        );

        when(
                repository.findBlockedSeatIds(
                        EVENT_ID,
                        candidates.seatIds()
                )
        ).thenReturn(
                Mono.just(
                        Set.of(
                                "A-01-01",
                                "A-01-02"
                        )
                )
        );

        StepVerifier
                .create(
                        service.hold(
                                EVENT_ID,
                                CUSTOMER_ID
                        )
                )
                .expectError(
                        NoAvailableSeatException.class
                )
                .verify();

        verify(
                repository,
                never()
        ).save(
                any()
        );

        verifyNoInteractions(
                eventPublisher
        );
    }

    @Test
    void shouldReturnNoAvailableSeatWhenAllSaveAttemptsLoseRace() {

        EventSeatCandidates candidates =
                new EventSeatCandidates(
                        EVENT_ID,
                        List.of(
                                "A-01-01",
                                "A-01-02"
                        )
                );

        when(
                eventSeatCatalog.getSeatCandidates(
                        EVENT_ID,
                        20
                )
        ).thenReturn(
                Mono.just(candidates)
        );

        when(
                repository.findBlockedSeatIds(
                        EVENT_ID,
                        candidates.seatIds()
                )
        ).thenReturn(
                Mono.just(
                        Set.of()
                )
        );

        when(
                repository.save(
                        any(Reservation.class)
                )
        ).thenAnswer(invocation -> {

            Reservation reservation =
                    invocation.getArgument(0);

            return Mono.error(
                    new SeatUnavailableException(
                            reservation.getSeatId()
                    )
            );
        });

        StepVerifier
                .create(
                        service.hold(
                                EVENT_ID,
                                CUSTOMER_ID
                        )
                )
                .expectError(
                        NoAvailableSeatException.class
                )
                .verify();

        verify(
                repository,
                times(2)
        ).save(
                any(Reservation.class)
        );

        verifyNoInteractions(
                eventPublisher
        );
    }

    @Test
    void shouldGetReservationById() {

        Reservation reservation =
                reservation(
                        RESERVATION_ID,
                        HELD
                );

        when(
                repository.findById(
                        RESERVATION_ID
                )
        ).thenReturn(
                Mono.just(reservation)
        );

        StepVerifier
                .create(
                        service.getById(
                                RESERVATION_ID
                        )
                )
                .expectNext(
                        reservation
                )
                .verifyComplete();
    }

    @Test
    void shouldFailWhenReservationDoesNotExist() {

        when(
                repository.findById(
                        RESERVATION_ID
                )
        ).thenReturn(
                Mono.empty()
        );

        StepVerifier
                .create(
                        service.getById(
                                RESERVATION_ID
                        )
                )
                .expectError(
                        ReservationNotFoundException.class
                )
                .verify();
    }

    @Test
    void shouldExpireDueReservationsAndPublishEvents() {

        Reservation first =
                expiredHeldReservation(
                        UUID.randomUUID(),
                        "A-01-01"
                );

        Reservation second =
                expiredHeldReservation(
                        UUID.randomUUID(),
                        "A-01-02"
                );

        when(
                repository.findExpiredHolds(
                        NOW
                )
        ).thenReturn(
                Flux.just(
                        first,
                        second
                )
        );

        when(
                repository.save(
                        any(Reservation.class)
                )
        ).thenAnswer(
                invocation ->
                        Mono.just(
                                invocation.getArgument(0)
                        )
        );

        when(
                eventPublisher.publish(
                        any(Reservation.class)
                )
        ).thenReturn(
                Mono.empty()
        );

        StepVerifier
                .create(
                        service.expireDueReservations()
                )
                .expectNext(2L)
                .verifyComplete();

        assertThat(
                first.getStatus()
        ).isEqualTo(EXPIRED);

        assertThat(
                second.getStatus()
        ).isEqualTo(EXPIRED);

        verify(
                repository,
                times(2)
        ).save(
                any(Reservation.class)
        );

        verify(
                eventPublisher,
                times(2)
        ).publish(
                any(Reservation.class)
        );
    }

    @Test
    void shouldReturnZeroWhenNoReservationIsDueForExpiration() {

        when(
                repository.findExpiredHolds(
                        NOW
                )
        ).thenReturn(
                Flux.empty()
        );

        StepVerifier
                .create(
                        service.expireDueReservations()
                )
                .expectNext(0L)
                .verifyComplete();

        verify(
                repository,
                never()
        ).save(
                any()
        );

        verifyNoInteractions(
                eventPublisher
        );
    }

    @Test
    void shouldConfirmReservationAndPublishEvent() {

        Reservation reservation =
                reservation(
                        RESERVATION_ID,
                        HELD
                );

        when(
                repository.findById(
                        RESERVATION_ID
                )
        ).thenReturn(
                Mono.just(reservation)
        );

        when(
                repository.save(
                        reservation
                )
        ).thenReturn(
                Mono.just(reservation)
        );

        when(
                eventPublisher.publish(
                        reservation
                )
        ).thenReturn(
                Mono.empty()
        );

        StepVerifier
                .create(
                        service.confirm(
                                RESERVATION_ID
                        )
                )
                .verifyComplete();

        assertThat(
                reservation.getStatus()
        ).isEqualTo(CONFIRMED);

        verify(
                repository
        ).save(
                reservation
        );

        verify(
                eventPublisher
        ).publish(
                reservation
        );
    }

    @Test
    void shouldFailConfirmWhenReservationDoesNotExist() {

        when(
                repository.findById(
                        RESERVATION_ID
                )
        ).thenReturn(
                Mono.empty()
        );

        StepVerifier
                .create(
                        service.confirm(
                                RESERVATION_ID
                        )
                )
                .expectError(
                        ReservationNotFoundException.class
                )
                .verify();

        verify(
                repository,
                never()
        ).save(
                any()
        );

        verifyNoInteractions(
                eventPublisher
        );
    }

    @Test
    void shouldCancelReservationAndPublishEvent() {

        Reservation reservation =
                reservation(
                        RESERVATION_ID,
                        HELD
                );

        when(
                repository.findById(
                        RESERVATION_ID
                )
        ).thenReturn(
                Mono.just(reservation)
        );

        when(
                repository.save(
                        reservation
                )
        ).thenReturn(
                Mono.just(reservation)
        );

        when(
                eventPublisher.publish(
                        reservation
                )
        ).thenReturn(
                Mono.empty()
        );

        StepVerifier
                .create(
                        service.cancel(
                                RESERVATION_ID
                        )
                )
                .verifyComplete();

        assertThat(
                reservation.getStatus()
        ).isEqualTo(CANCELLED);

        verify(
                repository
        ).save(
                reservation
        );

        verify(
                eventPublisher
        ).publish(
                reservation
        );
    }

    @Test
    void shouldFailCancelWhenReservationDoesNotExist() {

        when(
                repository.findById(
                        RESERVATION_ID
                )
        ).thenReturn(
                Mono.empty()
        );

        StepVerifier
                .create(
                        service.cancel(
                                RESERVATION_ID
                        )
                )
                .expectError(
                        ReservationNotFoundException.class
                )
                .verify();

        verify(
                repository,
                never()
        ).save(
                any()
        );

        verifyNoInteractions(
                eventPublisher
        );
    }

    @Test
    void shouldFindReservationsByCustomerId() {

        Reservation first =
                reservation(
                        UUID.randomUUID(),
                        HELD
                );

        Reservation second =
                reservation(
                        UUID.randomUUID(),
                        CONFIRMED
                );

        when(
                repository.findByCustomerId(
                        CUSTOMER_ID
                )
        ).thenReturn(
                Flux.just(
                        first,
                        second
                )
        );

        StepVerifier
                .create(
                        service.findByCustomerId(
                                CUSTOMER_ID
                        )
                )
                .expectNext(
                        first,
                        second
                )
                .verifyComplete();

        verify(
                repository
        ).findByCustomerId(
                CUSTOMER_ID
        );
    }

    @Test
    void shouldPropagatePublisherFailure() {

        Reservation reservation =
                reservation(
                        RESERVATION_ID,
                        HELD
                );

        RuntimeException failure =
                new RuntimeException(
                        "Kafka unavailable"
                );

        when(
                repository.findById(
                        RESERVATION_ID
                )
        ).thenReturn(
                Mono.just(reservation)
        );

        when(
                repository.save(
                        reservation
                )
        ).thenReturn(
                Mono.just(reservation)
        );

        when(
                eventPublisher.publish(
                        reservation
                )
        ).thenReturn(
                Mono.error(failure)
        );

        StepVerifier
                .create(
                        service.cancel(
                                RESERVATION_ID
                        )
                )
                .expectErrorMatches(
                        exception ->
                                exception == failure
                )
                .verify();
    }

    private Reservation reservation(
            UUID reservationId,
            ReservationStatus status
    ) {

        return Reservation.restore(
                reservationId,
                EVENT_ID,
                "A-01-01",
                CUSTOMER_ID,
                status,
                NOW.minus(
                        Duration.ofMinutes(1)
                ),
                NOW.plus(
                        Duration.ofMinutes(9)
                )
        );
    }

    private Reservation expiredHeldReservation(
            UUID reservationId,
            String seatId
    ) {

        return Reservation.restore(
                reservationId,
                EVENT_ID,
                seatId,
                CUSTOMER_ID,
                HELD,
                NOW.minus(
                        Duration.ofMinutes(20)
                ),
                NOW.minus(
                        Duration.ofMinutes(10)
                )
        );
    }
}