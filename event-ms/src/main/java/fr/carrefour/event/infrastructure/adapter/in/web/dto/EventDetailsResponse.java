package fr.carrefour.event.infrastructure.adapter.in.web.dto;

import fr.carrefour.event.application.model.EventDetails;

import java.time.Instant;
import java.util.UUID;

public record EventDetailsResponse(
        UUID id,
        String name,
        String venue,
        Instant startsAt
) {

    public static EventDetailsResponse from(
            EventDetails event
    ) {
        return new EventDetailsResponse(
                event.id(),
                event.name(),
                event.venue(),
                event.startsAt()
        );
    }
}
