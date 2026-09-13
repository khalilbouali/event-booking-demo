package fr.carrefour.event.application.port.in;

import fr.carrefour.event.application.model.EventSeatCandidates;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface GetEventSeatCandidatesUseCase {

    Mono<EventSeatCandidates> getSeatCandidates(
            UUID eventId,
            int limit
    );
}
