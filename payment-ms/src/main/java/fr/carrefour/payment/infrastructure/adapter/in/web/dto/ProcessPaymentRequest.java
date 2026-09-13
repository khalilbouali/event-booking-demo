package fr.carrefour.payment.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ProcessPaymentRequest(

        @NotNull
        UUID reservationId,

        @NotNull
        @DecimalMin(
                value = "0.01",
                message = "Amount must be greater than zero"
        )
        BigDecimal amount,
        boolean isValid // to remove, letting the user decide the validity of each payment for e2e tests
) {
}
