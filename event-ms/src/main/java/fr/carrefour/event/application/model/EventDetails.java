package fr.carrefour.event.application.model;

import java.time.Instant;
import java.util.UUID;

public record EventDetails(
        UUID id,
        String name,
        String venue,
        Instant startsAt
) {
}
