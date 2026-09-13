package fr.carrefour.reservation.application.port.out;

import fr.carrefour.reservation.domain.model.Reservation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface ReservationRepository {

    Mono<Reservation> save(Reservation reservation);

    Mono<Reservation> findById(UUID reservationId);

    Flux<Reservation> findExpiredHolds(Instant now);

    Mono<Set<String>> findBlockedSeatIds(
            UUID eventId,
            Collection<String> seatIds
    );

    Flux<Reservation> findByCustomerId(String customerId);
}