package fr.carrefour.event.infrastructure.adapter.out.persistence.mongodb;

import fr.carrefour.event.application.port.out.EventRepository;
import fr.carrefour.event.domain.model.Event;
import fr.carrefour.event.infrastructure.adapter.out.persistence.mongodb.repository.ReactiveMongoEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MongoEventRepositoryAdapter implements EventRepository {

    private final ReactiveMongoEventRepository repository;
    private final EventPersistenceMapper mapper;

    @Override
    public Mono<Event> save(Event event) {
        return repository
                .save(mapper.toDocument(event))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Event> findById(UUID eventId) {
        return repository
                .findById(eventId)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Event> findAll() {
        return repository
                .findAll()
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Event> findAllById(Collection<UUID> ids) {
        return repository
                .findAllById(ids)
                .map(mapper::toDomain);
    }
}
