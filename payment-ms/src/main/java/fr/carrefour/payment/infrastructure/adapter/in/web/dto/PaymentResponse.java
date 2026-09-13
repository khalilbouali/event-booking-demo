package fr.carrefour.payment.infrastructure.adapter.in.web.dto;

import fr.carrefour.payment.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID reservationId,
        String customerId,
        BigDecimal amount,
        PaymentStatus status,
        Instant createdAt
) {
}
