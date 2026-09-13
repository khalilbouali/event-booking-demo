package fr.carrefour.bff.infrastructure.adapter.io.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID reservationId,
        String customerId,
        BigDecimal amount,
        String status,
        Instant createdAt
) {
}
