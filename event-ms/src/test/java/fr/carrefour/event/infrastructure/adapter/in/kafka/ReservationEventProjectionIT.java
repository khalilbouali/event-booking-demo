package fr.carrefour.event.infrastructure.adapter.in.kafka;

import fr.carrefour.event.application.port.out.EventAvailabilityRepository;
import fr.carrefour.event.domain.model.EventAvailability;
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

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "app.kafka.topics.reservation-events=reservation-events-projection-it",

                "spring.kafka.consumer.group-id=event-projection-it",
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
class ReservationEventProjectionIT {

    private static final String TOPIC =
            "reservation-events-projection-it";

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

    @Autowired
    private EventAvailabilityRepository
            availabilityRepository;

    @MockitoBean
    private ReactiveJwtDecoder
            reactiveJwtDecoder;

    @Test
    void shouldUpdateAvailabilityProjectionThroughKafkaLifecycle()
            throws Exception {

        UUID eventId =
                UUID.randomUUID();

        String seatId =
                "A-01-01";

        /*
         * Initial projection:
         *
         * totalSeats     = 2
         * remainingSeats = 2
         * blocked seats  = []
         */
        availabilityRepository
                .initialize(
                        eventId,
                        2
                )
                .block(
                        Duration.ofSeconds(5)
                );

        /*
         * HELD
         *
         * The seat becomes unavailable.
         */
        sendEvent(
                eventId,
                seatId,
                "HELD"
        );

        awaitAvailability(
                eventId,
                1
        );

        awaitBlockedSeat(
                eventId,
                seatId,
                true
        );

        /*
         * CONFIRMED
         *
         * Nothing changes because HELD already blocked the seat.
         */
        sendEvent(
                eventId,
                seatId,
                "CONFIRMED"
        );

        awaitAvailability(
                eventId,
                1
        );

        awaitBlockedSeat(
                eventId,
                seatId,
                true
        );

        /*
         * EXPIRED
         *
         * The seat becomes available again.
         */
        sendEvent(
                eventId,
                seatId,
                "EXPIRED"
        );

        awaitAvailability(
                eventId,
                2
        );

        awaitBlockedSeat(
                eventId,
                seatId,
                false
        );
    }

    private void sendEvent(
            UUID eventId,
            String seatId,
            String status
    ) throws Exception {

        String payload =
                """
                {
                  "eventId": "%s",
                  "seatId": "%s",
                  "status": "%s"
                }
                """.formatted(
                        eventId,
                        seatId,
                        status
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
    }

    private void awaitAvailability(
            UUID eventId,
            int expectedRemainingSeats
    ) {

        Mono<EventAvailability> waitForExpectedState =
                Mono.defer(
                                () ->
                                        availabilityRepository
                                                .findByEventId(
                                                        eventId
                                                )
                        )
                        .filter(
                                availability ->
                                        availability.remainingSeats()
                                                == expectedRemainingSeats
                        )
                        .repeatWhenEmpty(
                                repeat ->
                                        repeat.delayElements(
                                                Duration.ofMillis(100)
                                        )
                        )
                        .timeout(
                                Duration.ofSeconds(10)
                        );

        EventAvailability availability =
                waitForExpectedState.block();

        assertThat(availability)
                .isNotNull();

        assertThat(
                availability.eventId()
        ).isEqualTo(
                eventId
        );

        assertThat(
                availability.remainingSeats()
        ).isEqualTo(
                expectedRemainingSeats
        );
    }

    private void awaitBlockedSeat(
            UUID eventId,
            String seatId,
            boolean expectedBlocked
    ) {

        Mono<Set<String>> waitForExpectedState =
                Mono.defer(
                                () ->
                                        availabilityRepository
                                                .findBlockedSeatIds(
                                                        eventId
                                                )
                        )
                        .filter(
                                blockedSeatIds ->
                                        blockedSeatIds.contains(
                                                seatId
                                        ) == expectedBlocked
                        )
                        .repeatWhenEmpty(
                                repeat ->
                                        repeat.delayElements(
                                                Duration.ofMillis(100)
                                        )
                        )
                        .timeout(
                                Duration.ofSeconds(10)
                        );

        Set<String> blockedSeatIds =
                waitForExpectedState.block();

        assertThat(blockedSeatIds)
                .isNotNull();

        assertThat(
                blockedSeatIds.contains(
                        seatId
                )
        ).isEqualTo(
                expectedBlocked
        );
    }
}