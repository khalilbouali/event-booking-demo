package fr.carrefour.reservation.infrastructure.adapter.out.persistence.mongodb;

import fr.carrefour.reservation.domain.model.ReservationStatus;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

public interface ReactiveMongoReservationRepository
        extends ReactiveCrudRepository<ReservationDocument, UUID> {

    Flux<ReservationDocument> findByStatusAndExpiresAtLessThanEqual(
            ReservationStatus status,
            Instant expiresAt
    );
    Flux<ReservationDocument> findByEventIdAndSeatIdInAndActiveTrue(
            UUID eventId,
            Collection<String> seatIds
    );

    Flux<ReservationDocument> findByCustomerIdOrderByCreatedAtDesc(
            String customerId
    );
}