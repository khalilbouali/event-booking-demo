package fr.carrefour.payment.application.port.out;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentProcessor {

    Mono<Boolean> process(
            UUID reservationId,
            BigDecimal amount,
            boolean isValid
    );
}
