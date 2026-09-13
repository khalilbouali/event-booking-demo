package fr.carrefour.payment.application.port.out;

import fr.carrefour.payment.domain.model.Payment;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PaymentRepository {

    Mono<Payment> save(Payment payment);

    Mono<Payment> findById(UUID paymentId);

    Flux<Payment> findByCustomerId(String customerId);
}