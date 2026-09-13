package fr.carrefour.notification.infrastructure.adapter.in.kafka;

import fr.carrefour.notification.application.port.in.SendNotificationUseCase;
import fr.carrefour.notification.infrastructure.adapter.in.kafka.event.ReservationEvent;
import fr.carrefour.notification.infrastructure.adapter.in.kafka.exception.InvalidReservationEventException;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import static fr.carrefour.notification.domain.model.NotificationType.*;

@Component
@RequiredArgsConstructor
public final class ReservationEventConsumer {

    private final JsonMapper jsonMapper;
    private final SendNotificationUseCase sendNotificationUseCase;

    @KafkaListener(
            topics = "${kafka.topics.reservation-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String payload) {

        ReservationEvent event = deserialize(payload);

        switch (event.status()) {

            case CONFIRMED ->
                    sendNotificationUseCase.send(
                            event.customerId(),
                            event.seatId(),
                            CONFIRMED
                    );

            case CANCELLED ->
                    sendNotificationUseCase.send(
                            event.customerId(),
                            event.seatId(),
                            CANCELLED
                    );

            case EXPIRED ->
                    sendNotificationUseCase.send(
                            event.customerId(),
                            event.seatId(),
                            EXPIRED
                    );

            case HELD -> {
                // No user notification for temporary seat holds.
            }
        }
    }

    private ReservationEvent deserialize(String payload) {
        try {
            return jsonMapper.readValue(
                    payload,
                    ReservationEvent.class
            );
        } catch (JacksonException exception) {
            throw new InvalidReservationEventException(
                    "Unable to deserialize reservation event",
                    exception
            );
        }
    }
}
