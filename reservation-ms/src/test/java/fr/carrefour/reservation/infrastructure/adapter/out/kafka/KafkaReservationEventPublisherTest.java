package fr.carrefour.reservation.infrastructure.adapter.out.kafka;

import fr.carrefour.reservation.domain.model.Reservation;
import fr.carrefour.reservation.infrastructure.adapter.out.kafka.event.ReservationEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static fr.carrefour.reservation.domain.model.ReservationStatus.HELD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaReservationEventPublisherTest {

    private static final String TOPIC =
            "reservation-events";

    private static final UUID RESERVATION_ID =
            UUID.fromString(
                    "11111111-1111-1111-1111-111111111111"
            );

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "22222222-2222-2222-2222-222222222222"
            );

    private static final String SEAT_ID =
            "A-01-01";

    private static final String CUSTOMER_ID =
            "customer-123";

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-10T10:00:00Z"
            );

    private static final String PAYLOAD =
            "{\"event\":\"reservation\"}";

    @Mock
    private KafkaTemplate<String, String>
            kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private KafkaReservationEventPublisher publisher;

    @BeforeEach
    void setUp() {

        Clock clock =
                Clock.fixed(
                        NOW,
                        ZoneOffset.UTC
                );

        publisher =
                new KafkaReservationEventPublisher(
                        kafkaTemplate,
                        objectMapper,
                        clock,
                        TOPIC
                );
    }

    @Test
    void shouldPublishReservationEventToKafka()
            throws Exception {

        Reservation reservation =
                reservation();

        when(
                objectMapper.writeValueAsString(
                        any(ReservationEvent.class)
                )
        ).thenReturn(
                PAYLOAD
        );

        when(
                kafkaTemplate.send(
                        TOPIC,
                        RESERVATION_ID.toString(),
                        PAYLOAD
                )
        ).thenReturn(
                successfulSend()
        );

        StepVerifier
                .create(
                        publisher.publish(
                                reservation
                        )
                )
                .verifyComplete();

        ArgumentCaptor<ReservationEvent> eventCaptor =
                ArgumentCaptor.forClass(
                        ReservationEvent.class
                );

        verify(
                objectMapper
        ).writeValueAsString(
                eventCaptor.capture()
        );

        ReservationEvent event =
                eventCaptor.getValue();

        assertThat(
                event.messageId()
        ).isNotNull();

        assertThat(
                event.reservationId()
        ).isEqualTo(
                RESERVATION_ID
        );

        assertThat(
                event.eventId()
        ).isEqualTo(
                EVENT_ID
        );

        assertThat(
                event.seatId()
        ).isEqualTo(
                SEAT_ID
        );

        assertThat(
                event.customerId()
        ).isEqualTo(
                CUSTOMER_ID
        );

        assertThat(
                event.status()
        ).isEqualTo(
                HELD
        );

        assertThat(
                event.occurredAt()
        ).isEqualTo(
                NOW
        );

        verify(
                kafkaTemplate
        ).send(
                TOPIC,
                RESERVATION_ID.toString(),
                PAYLOAD
        );
    }

    @Test
    void shouldUseDifferentMessageIdForEachPublishedEvent()
            throws Exception {

        Reservation reservation =
                reservation();

        when(
                objectMapper.writeValueAsString(
                        any(ReservationEvent.class)
                )
        ).thenReturn(
                PAYLOAD
        );

        when(
                kafkaTemplate.send(
                        TOPIC,
                        RESERVATION_ID.toString(),
                        PAYLOAD
                )
        ).thenReturn(
                successfulSend()
        );

        StepVerifier
                .create(
                        publisher.publish(reservation)
                                .then(
                                        publisher.publish(
                                                reservation
                                        )
                                )
                )
                .verifyComplete();

        ArgumentCaptor<ReservationEvent> eventCaptor =
                ArgumentCaptor.forClass(
                        ReservationEvent.class
                );

        verify(
                objectMapper,
                times(2)
        ).writeValueAsString(
                eventCaptor.capture()
        );

        assertThat(
                eventCaptor
                        .getAllValues()
                        .get(0)
                        .messageId()
        ).isNotEqualTo(
                eventCaptor
                        .getAllValues()
                        .get(1)
                        .messageId()
        );
    }

    @Test
    void shouldBeLazyUntilSubscribed()
            throws Exception {

        Reservation reservation =
                reservation();

        Mono<Void> result =
                publisher.publish(
                        reservation
                );

        verifyNoInteractions(
                objectMapper,
                kafkaTemplate
        );

        when(
                objectMapper.writeValueAsString(
                        any(ReservationEvent.class)
                )
        ).thenReturn(
                PAYLOAD
        );

        when(
                kafkaTemplate.send(
                        TOPIC,
                        RESERVATION_ID.toString(),
                        PAYLOAD
                )
        ).thenReturn(
                successfulSend()
        );

        StepVerifier
                .create(result)
                .verifyComplete();

        verify(
                kafkaTemplate
        ).send(
                TOPIC,
                RESERVATION_ID.toString(),
                PAYLOAD
        );
    }

    @Test
    void shouldWrapSerializationFailure()
            throws Exception {

        Reservation reservation =
                reservation();

        JacksonException failure =
                mock(
                        JacksonException.class
                );

        when(
                objectMapper.writeValueAsString(
                        any(ReservationEvent.class)
                )
        ).thenThrow(
                failure
        );

        StepVerifier
                .create(
                        publisher.publish(
                                reservation
                        )
                )
                .expectErrorSatisfies(exception -> {

                    assertThat(exception)
                            .isInstanceOf(
                                    IllegalStateException.class
                            );

                    assertThat(exception)
                            .hasMessage(
                                    "Failed to serialize reservation event"
                            );

                    assertThat(
                            exception.getCause()
                    ).isSameAs(
                            failure
                    );
                })
                .verify();

        verifyNoInteractions(
                kafkaTemplate
        );
    }

    @Test
    void shouldPropagateKafkaSendFailure()
            throws Exception {

        Reservation reservation =
                reservation();

        RuntimeException failure =
                new RuntimeException(
                        "Kafka unavailable"
                );

        when(
                objectMapper.writeValueAsString(
                        any(ReservationEvent.class)
                )
        ).thenReturn(
                PAYLOAD
        );

        CompletableFuture<SendResult<String, String>>
                future =
                new CompletableFuture<>();

        future.completeExceptionally(
                failure
        );

        when(
                kafkaTemplate.send(
                        TOPIC,
                        RESERVATION_ID.toString(),
                        PAYLOAD
                )
        ).thenReturn(
                future
        );

        StepVerifier
                .create(
                        publisher.publish(
                                reservation
                        )
                )
                .expectErrorMatches(
                        exception ->
                                exception == failure
                )
                .verify();
    }

    private Reservation reservation() {

        return Reservation.restore(
                RESERVATION_ID,
                EVENT_ID,
                SEAT_ID,
                CUSTOMER_ID,
                HELD,
                NOW.minus(
                        Duration.ofMinutes(1)
                ),
                NOW.plus(
                        Duration.ofMinutes(9)
                )
        );
    }

    private CompletableFuture<SendResult<String, String>>
    successfulSend() {

        ProducerRecord<String, String> producerRecord =
                new ProducerRecord<>(
                        TOPIC,
                        RESERVATION_ID.toString(),
                        PAYLOAD
                );

        SendResult<String, String> sendResult =
                new SendResult<>(
                        producerRecord,
                        null
                );

        return CompletableFuture.completedFuture(
                sendResult
        );
    }
}