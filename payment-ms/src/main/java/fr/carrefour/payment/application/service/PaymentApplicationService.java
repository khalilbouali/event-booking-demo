package fr.carrefour.payment.application.service;

import fr.carrefour.payment.application.port.in.GetPaymentUseCase;
import fr.carrefour.payment.application.port.in.ListMyPaymentsUseCase;
import fr.carrefour.payment.application.port.in.ProcessPaymentUseCase;
import fr.carrefour.payment.application.port.out.PaymentEventPublisher;
import fr.carrefour.payment.application.port.out.PaymentProcessor;
import fr.carrefour.payment.application.port.out.PaymentRepository;
import fr.carrefour.payment.domain.exception.PaymentAccessDeniedException;
import fr.carrefour.payment.domain.exception.PaymentNotFoundException;
import fr.carrefour.payment.domain.model.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

import static fr.carrefour.payment.domain.model.Payment.create;
import static reactor.core.publisher.Mono.error;

@Service
@RequiredArgsConstructor
public class PaymentApplicationService
        implements ProcessPaymentUseCase, GetPaymentUseCase, ListMyPaymentsUseCase {

    private final PaymentRepository repository;
    private final PaymentProcessor paymentProcessor;
    private final PaymentEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public Mono<Payment> process(
            UUID reservationId,
            String customerId,
            BigDecimal amount,
            boolean isValid
    ) {

        Payment payment = create(
                reservationId,
                customerId,
                amount,
                clock.instant(),
                isValid
        );

        return repository.save(payment)
                .flatMap(saved ->
                        paymentProcessor.process(
                                        saved.getReservationId(),
                                        saved.getAmount(),
                                        isValid
                                )
                                .flatMap(success -> {
                                    applyProcessingResult(payment, success);
                                    return repository.save(saved);
                                })
                )
                .flatMap(saved ->
                        eventPublisher.publish(saved)
                                .thenReturn(saved)
                );
    }

    @Override
    public Mono<Payment> getById(UUID paymentId, String customerId) {
        return repository.findById(paymentId)
                .switchIfEmpty(
                        error(
                                new PaymentNotFoundException(paymentId)
                        )
                )
                .filter(payment ->
                        payment.getCustomerId().equals(customerId)
                )
                .switchIfEmpty(
                        error(
                                new PaymentAccessDeniedException(paymentId)
                        )
                );
    }

    @Override
    public Flux<Payment> findByCustomerId(
            String customerId
    ) {
        return repository
                .findByCustomerId(customerId);
    }

    private void applyProcessingResult(
            Payment payment,
            boolean success
    ) {
        if (success) {
            payment.succeed();
        } else {
            payment.fail();
        }
    }
}
