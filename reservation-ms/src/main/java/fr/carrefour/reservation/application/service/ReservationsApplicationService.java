package fr.carrefour.reservation.application.service;

import fr.carrefour.reservation.application.port.in.*;
import fr.carrefour.reservation.application.port.out.EventSeatCatalog;
import fr.carrefour.reservation.application.port.out.ReservationEventPublisher;
import fr.carrefour.reservation.application.port.out.ReservationRepository;
import fr.carrefour.reservation.domain.exception.NoAvailableSeatException;
import fr.carrefour.reservation.domain.exception.ReservationNotFoundException;
import fr.carrefour.reservation.domain.exception.SeatUnavailableException;
import fr.carrefour.reservation.domain.model.Reservation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static reactor.core.publisher.Flux.fromIterable;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;

@Service
public final class ReservationsApplicationService
        implements HoldSeatUseCase, GetReservationUseCase, ExpireReservationsUseCase,
            ConfirmReservationUseCase, CancelReservationUseCase, ListMyReservationsUseCase {

    private static final int CANDIDATE_LIMIT = 20;
    private final ReservationRepository repository;
    private final ReservationEventPublisher eventPublisher;
    private final EventSeatCatalog eventSeatCatalog;
    private final Clock clock;

    private final Duration reservationHoldDuration;

    public ReservationsApplicationService(
            ReservationRepository repository,
            ReservationEventPublisher eventPublisher,
            EventSeatCatalog eventSeatCatalog,
            Clock clock,
            @Qualifier("reservationHoldDuration")
            Duration reservationHoldDuration
    ) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.eventSeatCatalog = eventSeatCatalog;
        this.clock = clock;
        this.reservationHoldDuration = reservationHoldDuration;
    }

    @Override
    public Mono<Reservation> hold(
            UUID eventId,
            String customerId
    ) {

        return eventSeatCatalog
                .getSeatCandidates(
                        eventId,
                        CANDIDATE_LIMIT
                )
                .flatMap(candidates -> {

                    if (candidates.seatIds().isEmpty()) {
                        return error(
                                new NoAvailableSeatException(
                                        eventId
                                )
                        );
                    }

                    return repository
                            .findBlockedSeatIds(
                                    eventId,
                                    candidates.seatIds()
                            )
                            .flatMapMany(blockedSeatIds ->
                                    fromIterable(
                                                    candidates.seatIds()
                                            )
                                            .filter(seatId ->
                                                    !blockedSeatIds.contains(
                                                            seatId
                                                    )
                                            )
                            )
                            .concatMap(seatId ->
                                    tryHoldSeat(
                                            eventId,
                                            seatId,
                                            customerId
                                    )
                                            .onErrorResume(
                                                    SeatUnavailableException.class,
                                                    exception ->
                                                            empty()
                                            )
                            )
                            .next()
                            .switchIfEmpty(
                                    error(
                                            new NoAvailableSeatException(
                                                    eventId
                                            )
                                    )
                            );
                });
    }

    @Override
    public Mono<Reservation> getById(UUID reservationId) {

        return repository
                .findById(reservationId)
                .switchIfEmpty(
                        error(
                                new ReservationNotFoundException(
                                        reservationId
                                )
                        )
                );
    }

    @Override
    public Mono<Long> expireDueReservations() {
        Instant now = clock.instant();

        return repository.findExpiredHolds(now)
                .flatMap(reservation -> {
                    reservation.expire(now);
                    return saveAndPublish(reservation);
                })
                .count();
    }

    @Override
    public Mono<Void> confirm(UUID reservationId) {
        Instant now = clock.instant();

        return repository.findById(reservationId)
                .switchIfEmpty(
                        error(
                                new ReservationNotFoundException(reservationId)
                        )
                )
                .flatMap(reservation -> {
                    reservation.confirm(now);

                    return saveAndPublish(reservation);
                })
                .then();
    }

    @Override
    public Mono<Void> cancel(UUID reservationId) {
        return repository.findById(reservationId)
                .switchIfEmpty(
                        error(
                                new ReservationNotFoundException(reservationId)
                        )
                )
                .flatMap(reservation -> {
                    reservation.cancel();

                    return saveAndPublish(reservation);
                })
                .then();
    }

    @Override
    public Flux<Reservation> findByCustomerId(
            String customerId
    ) {
        return repository.findByCustomerId(customerId);
    }

    private Mono<Reservation> saveAndPublish(
            Reservation reservation
    ) {
        return repository.save(reservation)
                .flatMap(saved ->
                        eventPublisher.publish(saved)
                                .thenReturn(saved)
                );
    }

    private Mono<Reservation> tryHoldSeat(
            UUID eventId,
            String seatId,
            String customerId
    ) {

        Instant now =
                clock.instant();

        Reservation reservation =
                Reservation.hold(
                        eventId,
                        seatId,
                        customerId,
                        now,
                        reservationHoldDuration
                );

        return saveAndPublish(reservation);
    }
}