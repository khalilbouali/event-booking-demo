package fr.carrefour.reservation.infrastructure.adapter.in.kafka;

import fr.carrefour.reservation.application.port.in.CancelReservationUseCase;
import fr.carrefour.reservation.application.port.in.ConfirmReservationUseCase;
import fr.carrefour.reservation.infrastructure.adapter.in.kafka.event.PaymentEvent;
import fr.carrefour.reservation.infrastructure.adapter.in.kafka.exception.InvalidPaymentEventException;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;

@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final ConfirmReservationUseCase confirmReservationUseCase;
    private final CancelReservationUseCase cancelReservationUseCase;

    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topics.payment-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String payload) {

        PaymentEvent event = deserialize(payload);

        switch (event.status()) {

            case SUCCEEDED ->
                    confirmReservationUseCase
                            .confirm(event.reservationId())
                            .block();

            case FAILED ->
                    cancelReservationUseCase
                            .cancel(event.reservationId())
                            .block();
        }
    }

    private PaymentEvent deserialize(String payload) {
        try {
            return objectMapper.readValue(
                    payload,
                    PaymentEvent.class
            );
        } catch (JacksonException exception) {
            throw new InvalidPaymentEventException(
                    "Invalid payment event payload",
                    exception
            );
        }
    }
}