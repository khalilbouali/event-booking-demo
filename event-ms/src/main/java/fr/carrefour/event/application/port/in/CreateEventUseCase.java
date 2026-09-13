package fr.carrefour.event.application.port.in;

import fr.carrefour.event.application.model.EventSummary;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

public interface CreateEventUseCase {

    Mono<EventSummary> createEvent(
            String name,
            String venue,
            Instant startsAt,
            int seatCount,
            BigDecimal price
    );
}
