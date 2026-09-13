package fr.carrefour.payment.infrastructure.adapter.out.persistence.mongodb;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface ReactiveMongoPaymentRepository
        extends ReactiveCrudRepository<PaymentDocument, UUID> {

    Flux<PaymentDocument> findByCustomerIdOrderByCreatedAtDesc(
            String customerId
    );
}