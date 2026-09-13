package fr.carrefour.payment.application.port.in;

import fr.carrefour.payment.domain.model.Payment;
import reactor.core.publisher.Flux;

public interface ListMyPaymentsUseCase {

    Flux<Payment> findByCustomerId(String customerId);
}