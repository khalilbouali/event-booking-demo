package fr.carrefour.event.application.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EventSummary(

        UUID id,

        String name,

        String venue,

        Instant startsAt,

        int seatCount,

        int remainingSeatCount,

        BigDecimal price
) {
}
