package fr.carrefour.event.infrastructure.adapter.in.web.dto;

import java.time.Instant;

public record ApiError(
        int status,
        String message,
        Instant timestamp
) {
}