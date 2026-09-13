package fr.carrefour.event.infrastructure.adapter.out.persistence.mongodb.repository;

import fr.carrefour.event.infrastructure.adapter.out.persistence.mongodb.document.EventDocument;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface ReactiveMongoEventRepository
        extends ReactiveCrudRepository<EventDocument, UUID> {
}
