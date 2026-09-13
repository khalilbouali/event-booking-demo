package fr.carrefour.payment.application.port.in;

import fr.carrefour.payment.domain.model.Payment;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface GetPaymentUseCase {

    Mono<Payment> getById(UUID paymentId, String customerId);
}