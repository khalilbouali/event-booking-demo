package fr.carrefour.reservation.infrastructure.adapter.out.persistence.mongodb;

import fr.carrefour.reservation.application.port.out.ReservationRepository;
import fr.carrefour.reservation.domain.exception.SeatUnavailableException;
import fr.carrefour.reservation.domain.model.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import static fr.carrefour.reservation.domain.model.ReservationStatus.HELD;
import static java.util.Set.of;
import static java.util.stream.Collectors.toSet;
import static reactor.core.publisher.Mono.just;

@Component
@RequiredArgsConstructor
public class MongoReservationRepositoryAdapter
        implements ReservationRepository {

    private final ReactiveMongoReservationRepository repository;
    private final ReservationPersistenceMapper mapper;

    @Override
    public Mono<Reservation> save(
            Reservation reservation) {

        return repository
                .save(mapper.toDocument(reservation))
                .map(mapper::toDomain)
                .onErrorMap(
                        DuplicateKeyException.class,
                        exception ->
                                new SeatUnavailableException(
                                        reservation.getSeatId()
                                )
                );
    }

    @Override
    public Mono<Reservation> findById(
            UUID reservationId) {

        return repository
                .findById(reservationId)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Reservation> findExpiredHolds(Instant now) {
        return repository
                .findByStatusAndExpiresAtLessThanEqual(
                        HELD,
                        now
                )
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Set<String>> findBlockedSeatIds(
            UUID eventId,
            Collection<String> seatIds
    ) {

        if (seatIds.isEmpty()) {
            return just(of());
        }

        return repository
                .findByEventIdAndSeatIdInAndActiveTrue(
                        eventId,
                        seatIds
                )
                .map(ReservationDocument::getSeatId)
                .collect(toSet());
    }

    @Override
    public Flux<Reservation> findByCustomerId(
            String customerId
    ) {
        return repository
                .findByCustomerIdOrderByCreatedAtDesc(customerId)
                .map(mapper::toDomain);
    }
}