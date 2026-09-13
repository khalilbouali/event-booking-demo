package fr.carrefour.event.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateEventRequest(

        @NotBlank
        String name,

        @NotBlank
        String venue,

        @NotNull
        Instant startsAt,

        @Min(1)
        @Max(25740)
        int seatCount,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal price
) {
}
