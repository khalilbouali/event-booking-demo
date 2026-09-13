package fr.carrefour.event.application.port.in;

import fr.carrefour.event.application.model.EventSummary;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface GetEventUseCase {

    Mono<EventSummary> getById(UUID eventId);
}
