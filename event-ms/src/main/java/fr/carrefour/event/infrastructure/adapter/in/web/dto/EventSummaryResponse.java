package fr.carrefour.event.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EventSummaryResponse(
        UUID id,
        String name,
        String venue,
        Instant startsAt,
        int seatCount,

        int remainingSeatCount,
        BigDecimal price
) {
}