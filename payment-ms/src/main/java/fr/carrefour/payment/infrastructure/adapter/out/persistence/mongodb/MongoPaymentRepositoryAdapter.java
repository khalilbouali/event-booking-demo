package fr.carrefour.payment.infrastructure.adapter.out.persistence.mongodb;

import fr.carrefour.payment.application.port.out.PaymentRepository;
import fr.carrefour.payment.domain.model.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MongoPaymentRepositoryAdapter
        implements PaymentRepository {

    private final ReactiveMongoPaymentRepository repository;
    private final PaymentPersistenceMapper mapper;

    @Override
    public Mono<Payment> save(Payment payment) {
        return repository
                .save(mapper.toDocument(payment))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Payment> findById(UUID paymentId) {
        return repository
                .findById(paymentId)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Payment> findByCustomerId(
            String customerId
    ) {
        return repository
                .findByCustomerIdOrderByCreatedAtDesc(customerId)
                .map(mapper::toDomain);
    }
}
