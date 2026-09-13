package fr.carrefour.event.infrastructure.adapter.in.kafka;

import fr.carrefour.event.application.model.ReservationLifecycleStatus;
import fr.carrefour.event.application.port.in.UpdateEventAvailabilityUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mongodb.MongoDBContainer;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
        properties = {
                "app.kafka.topics.reservation-events=reservation-events-it",

                "spring.kafka.consumer.group-id=event-service-it",
                "spring.kafka.consumer.auto-offset-reset=earliest",

                "spring.kafka.consumer.key-deserializer="
                        + "org.apache.kafka.common.serialization.StringDeserializer",

                "spring.kafka.consumer.value-deserializer="
                        + "org.apache.kafka.common.serialization.StringDeserializer",

                "spring.kafka.producer.key-serializer="
                        + "org.apache.kafka.common.serialization.StringSerializer",

                "spring.kafka.producer.value-serializer="
                        + "org.apache.kafka.common.serialization.StringSerializer",

                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri="
                        + "http://localhost/mock-jwks",

                "spring.security.oauth2.resourceserver.jwt.issuer-uri="
                        + "http://localhost/mock-issuer",

                "security.oauth2.client-id=carrefour-kata-id",

                "spring.mongodb.representation.uuid=standard"
        }
)
@Testcontainers
class ReservationEventConsumerIT {

    private static final String TOPIC =
            "reservation-events-it";

    @Container
    @ServiceConnection
    static KafkaContainer kafka =
            new KafkaContainer(
                    "apache/kafka-native:3.8.0"
            );

    @Container
    @ServiceConnection
    static MongoDBContainer mongo =
            new MongoDBContainer(
                    "mongo:8.0"
            );

    @Autowired
    private KafkaTemplate<String, String>
            kafkaTemplate;

    @MockitoBean
    private UpdateEventAvailabilityUseCase
            updateEventAvailabilityUseCase;

    @MockitoBean
    private ReactiveJwtDecoder
            reactiveJwtDecoder;

    @Test
    void shouldConsumeReservationEventFromKafka()
            throws Exception {

        UUID eventId =
                UUID.randomUUID();

        String seatId =
                "A-01-01";

        CountDownLatch latch =
                new CountDownLatch(1);

        when(
                updateEventAvailabilityUseCase
                        .updateAvailability(
                                eventId,
                                seatId,
                                ReservationLifecycleStatus.HELD
                        )
        ).thenAnswer(invocation -> {

            latch.countDown();

            return Mono.empty();
        });

        String payload =
                """
                {
                  "eventId": "%s",
                  "seatId": "%s",
                  "status": "HELD"
                }
                """.formatted(
                        eventId,
                        seatId
                );

        kafkaTemplate
                .send(
                        TOPIC,
                        eventId.toString(),
                        payload
                )
                .get(
                        10,
                        TimeUnit.SECONDS
                );

        boolean consumed =
                latch.await(
                        10,
                        TimeUnit.SECONDS
                );

        assertThat(consumed)
                .isTrue();

        verify(
                updateEventAvailabilityUseCase
        ).updateAvailability(
                eventId,
                seatId,
                ReservationLifecycleStatus.HELD
        );
    }
}