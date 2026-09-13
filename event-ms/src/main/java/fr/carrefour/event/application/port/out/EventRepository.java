package fr.carrefour.event.application.port.out;

import fr.carrefour.event.domain.model.Event;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.UUID;

public interface EventRepository {

    Mono<Event> save(Event event);

    Mono<Event> findById(UUID eventId);

    Flux<Event> findAll();

    Flux<Event> findAllById(
            Collection<UUID> ids
    );
}
