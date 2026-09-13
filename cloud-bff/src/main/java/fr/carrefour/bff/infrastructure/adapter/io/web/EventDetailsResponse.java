package fr.carrefour.bff.infrastructure.adapter.io.web;

import java.time.Instant;
import java.util.UUID;

public record EventDetailsResponse(
        UUID id,
        String name,
        String venue,
        Instant startsAt
) {
}
