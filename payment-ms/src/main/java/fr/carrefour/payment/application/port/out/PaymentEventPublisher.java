package fr.carrefour.payment.application.port.out;

import fr.carrefour.payment.domain.model.Payment;
import reactor.core.publisher.Mono;

public interface PaymentEventPublisher {

    Mono<Void> publish(Payment payment);
}
