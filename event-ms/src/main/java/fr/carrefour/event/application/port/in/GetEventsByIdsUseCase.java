package fr.carrefour.event.application.port.in;

import fr.carrefour.event.application.model.EventDetails;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.UUID;

public interface GetEventsByIdsUseCase {

    Flux<EventDetails> getByIds(
            Collection<UUID> eventIds
    );
}
