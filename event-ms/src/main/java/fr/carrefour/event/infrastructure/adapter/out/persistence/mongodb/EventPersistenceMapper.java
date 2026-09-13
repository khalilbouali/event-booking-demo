package fr.carrefour.event.infrastructure.adapter.out.persistence.mongodb;

import fr.carrefour.event.domain.model.Event;
import fr.carrefour.event.infrastructure.adapter.out.persistence.mongodb.document.EventDocument;
import org.springframework.stereotype.Component;

@Component
public class EventPersistenceMapper {

    public EventDocument toDocument(Event event) {
        return new EventDocument(
                event.getId(),
                event.getName(),
                event.getVenue(),
                event.getStartsAt(),
                event.getSeats(),
                event.getPrice()
        );
    }

    public Event toDomain(EventDocument document) {
        return Event.restore(
                document.getId(),
                document.getName(),
                document.getVenue(),
                document.getStartsAt(),
                document.getSeats(),
                document.getPrice()
        );
    }
}
