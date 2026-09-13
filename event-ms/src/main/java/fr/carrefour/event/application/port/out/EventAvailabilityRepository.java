package fr.carrefour.event.application.port.out;

import fr.carrefour.event.domain.model.EventAvailability;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;

public interface EventAvailabilityRepository {

    Mono<Void> initialize(
            UUID eventId,
            int totalSeats
    );

    Mono<Void> blockSeat(
            UUID eventId,
            String seatId
    );

    Mono<Void> releaseSeat(
            UUID eventId,
            String seatId
    );

    Mono<EventAvailability> findByEventId(
            UUID eventId
    );

    Mono<Set<String>> findBlockedSeatIds(
            UUID eventId
    );
}
