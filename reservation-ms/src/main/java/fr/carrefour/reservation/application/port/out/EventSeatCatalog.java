package fr.carrefour.reservation.application.port.out;

import fr.carrefour.reservation.application.model.EventSeatCandidates;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface EventSeatCatalog {

    Mono<EventSeatCandidates> getSeatCandidates(
            UUID eventId,
            int limit
    );
}
