package fr.carrefour.payment.infrastructure.adapter.out.payment;

import fr.carrefour.payment.application.port.out.PaymentProcessor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

import static reactor.core.publisher.Mono.just;

@Component
public class SimulatedPaymentProcessor
        implements PaymentProcessor {

    @Override
    public Mono<Boolean> process(
            UUID reservationId,
            BigDecimal amount,
            boolean isValid
    ) {
        return just(isValid);
    }
}
