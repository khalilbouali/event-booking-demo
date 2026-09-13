package fr.carrefour.reservation.infrastructure.adapter.out.kafka;

import fr.carrefour.reservation.application.port.out.ReservationEventPublisher;
import fr.carrefour.reservation.domain.model.Reservation;
import fr.carrefour.reservation.infrastructure.adapter.out.kafka.event.ReservationEvent;
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
public class KafkaReservationEventPublisher
        implements ReservationEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String reservationEventsTopic;

    public KafkaReservationEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${kafka.topics.reservation-events}")
            String reservationEventsTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.reservationEventsTopic = reservationEventsTopic;
    }

    @Override
    public Mono<Void> publish(Reservation reservation) {

        return fromCallable(() -> toEvent(reservation))
                .map(this::serialize)
                .flatMap(payload ->
                        fromFuture(
                                kafkaTemplate.send(
                                        reservationEventsTopic,
                                        reservation.getId().toString(),
                                        payload
                                )
                        )
                )
                .then();
    }

    private ReservationEvent toEvent(Reservation reservation) {
        return new ReservationEvent(
                randomUUID(),
                reservation.getId(),
                reservation.getEventId(),
                reservation.getSeatId(),
                reservation.getCustomerId(),
                reservation.getStatus(),
                clock.instant()
        );
    }

    private String serialize(ReservationEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Failed to serialize reservation event",
                    exception
            );
        }
    }
}