package fr.carrefour.event.infrastructure.adapter.in.web;

import fr.carrefour.event.application.model.EventSummary;
import fr.carrefour.event.infrastructure.adapter.in.web.dto.EventSummaryResponse;

final class EventWebMapper {

    private EventWebMapper() {
    }

    static EventSummaryResponse toResponse(
            EventSummary event
    ) {

        return new EventSummaryResponse(
                event.id(),
                event.name(),
                event.venue(),
                event.startsAt(),
                event.seatCount(),
                event.remainingSeatCount(),
                event.price()
        );
    }
}
