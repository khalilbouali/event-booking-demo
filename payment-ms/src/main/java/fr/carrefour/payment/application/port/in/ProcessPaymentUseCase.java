package fr.carrefour.payment.application.port.in;

import fr.carrefour.payment.domain.model.Payment;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProcessPaymentUseCase {

    Mono<Payment> process(
            UUID reservationId,
            String customerId,
            BigDecimal amount,
            boolean isValid // to remove, letting the user decide the validity of each payment for e2e tests
    );
}