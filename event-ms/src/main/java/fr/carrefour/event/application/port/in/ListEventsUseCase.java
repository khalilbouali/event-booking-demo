package fr.carrefour.event.application.port.in;

import fr.carrefour.event.application.model.EventSummary;
import reactor.core.publisher.Flux;

public interface ListEventsUseCase {

    Flux<EventSummary> findAll();
}
