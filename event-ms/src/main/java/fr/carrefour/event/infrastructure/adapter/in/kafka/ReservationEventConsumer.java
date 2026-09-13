package fr.carrefour.event.infrastructure.adapter.in.kafka;

import fr.carrefour.event.application.model.ReservationLifecycleStatus;
import fr.carrefour.event.application.port.in.UpdateEventAvailabilityUseCase;
import fr.carrefour.event.infrastructure.adapter.in.kafka.dto.ReservationEventMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

import static fr.carrefour.event.application.model.ReservationLifecycleStatus.valueOf;
import static java.time.Duration.ofSeconds;
import static java.util.Locale.ROOT;

@Component
@RequiredArgsConstructor
public class ReservationEventConsumer {

    private static final Duration PROCESSING_TIMEOUT =
            ofSeconds(5);

    private final UpdateEventAvailabilityUseCase
            updateEventAvailabilityUseCase;

    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.reservation-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String payload) {

        ReservationEventMessage event =
                deserialize(payload);

        ReservationLifecycleStatus status =
                valueOf(
                        event.status()
                                .toUpperCase(ROOT)
                );

        updateEventAvailabilityUseCase
                .updateAvailability(
                        event.eventId(),
                        event.seatId(),
                        status
                )
                .block(PROCESSING_TIMEOUT);
    }

    private ReservationEventMessage deserialize(
            String payload
    ) {
        try {
            return objectMapper.readValue(
                    payload,
                    ReservationEventMessage.class
            );
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "Invalid reservation event payload",
                    exception
            );
        }
    }
}
