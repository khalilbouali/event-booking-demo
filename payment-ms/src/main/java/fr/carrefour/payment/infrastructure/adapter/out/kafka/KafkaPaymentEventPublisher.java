package fr.carrefour.payment.infrastructure.adapter.out.kafka;

import fr.carrefour.payment.application.port.out.PaymentEventPublisher;
import fr.carrefour.payment.domain.model.Payment;
import fr.carrefour.payment.infrastructure.adapter.out.kafka.event.PaymentEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;

import static java.util.UUID.randomUUID;
import static reactor.core.publisher.Mono.fromCallable;
import static reactor.core.publisher.Mono.fromFuture;

@Component
public class KafkaPaymentEventPublisher
        implements PaymentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Value("${kafka.topics.payment-events}")
    private final String paymentEventsTopic;

    public KafkaPaymentEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${kafka.topics.payment-events}")
            String paymentEventsTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.paymentEventsTopic = paymentEventsTopic;
    }

    @Override
    public Mono<Void> publish(Payment payment) {

        PaymentEvent event = toEvent(payment);

        return fromCallable(() -> serialize(event))
                .flatMap(payload ->
                        fromFuture(
                                kafkaTemplate.send(
                                        paymentEventsTopic,
                                        payment.getReservationId().toString(),
                                        payload
                                )
                        )
                )
                .then();
    }

    private PaymentEvent toEvent(Payment payment) {
        return new PaymentEvent(
                randomUUID(),
                payment.getId(),
                payment.getReservationId(),
                payment.getStatus(),
                clock.instant()
        );
    }

    private String serialize(PaymentEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Failed to serialize payment event",
                    exception
            );
        }
    }
}
