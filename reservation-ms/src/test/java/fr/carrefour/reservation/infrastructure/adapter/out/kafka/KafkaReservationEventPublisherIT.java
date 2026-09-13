package fr.carrefour.reservation.infrastructure.adapter.out.kafka;

import fr.carrefour.reservation.domain.model.Reservation;
import fr.carrefour.reservation.infrastructure.adapter.out.kafka.event.ReservationEvent;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static fr.carrefour.reservation.domain.model.ReservationStatus.HELD;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class KafkaReservationEventPublisherIT {

    private static final String TOPIC =
            "reservation-events-test";

    private static final UUID RESERVATION_ID =
            UUID.fromString(
                    "11111111-1111-1111-1111-111111111111"
            );

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "22222222-2222-2222-2222-222222222222"
            );

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-10T10:00:00Z"
            );

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(
                    DockerImageName.parse(
                            "apache/kafka-native:3.9.1"
                    )
            );

    private DefaultKafkaProducerFactory<String, String>
            producerFactory;

    private KafkaReservationEventPublisher publisher;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {

        createTopic();

        Map<String, Object> producerProperties =
                new HashMap<>();

        producerProperties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                KAFKA.getBootstrapServers()
        );

        producerProperties.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        producerProperties.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        producerProperties.put(
                ProducerConfig.ACKS_CONFIG,
                "all"
        );

        producerFactory =
                new DefaultKafkaProducerFactory<>(
                        producerProperties
                );

        KafkaTemplate<String, String> kafkaTemplate =
                new KafkaTemplate<>(
                        producerFactory
                );

        objectMapper =
                JsonMapper.builder()
                        .findAndAddModules()
                        .build();

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

    @AfterEach
    void tearDown() {

        if (producerFactory != null) {
            producerFactory.destroy();
        }
    }

    @Test
    void shouldPublishReservationEventToRealKafkaBroker() {

        Reservation reservation =
                Reservation.restore(
                        RESERVATION_ID,
                        EVENT_ID,
                        "A-01-01",
                        "customer-123",
                        HELD,
                        NOW.minus(
                                Duration.ofMinutes(1)
                        ),
                        NOW.plus(
                                Duration.ofMinutes(9)
                        )
                );

        try (
                KafkaConsumer<String, String> consumer =
                        createConsumer()
        ) {

            consumer.subscribe(
                    List.of(TOPIC)
            );

            publisher
                    .publish(reservation)
                    .block(
                            Duration.ofSeconds(10)
                    );

            ConsumerRecord<String, String> record =
                    awaitRecord(
                            consumer
                    );

            assertThat(
                    record.topic()
            ).isEqualTo(
                    TOPIC
            );

            assertThat(
                    record.key()
            ).isEqualTo(
                    RESERVATION_ID.toString()
            );

            ReservationEvent event =
                    objectMapper.readValue(
                            record.value(),
                            ReservationEvent.class
                    );

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
                    "A-01-01"
            );

            assertThat(
                    event.customerId()
            ).isEqualTo(
                    "customer-123"
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
        }
    }

    private KafkaConsumer<String, String>
    createConsumer() {

        Map<String, Object> consumerProperties =
                new HashMap<>();

        consumerProperties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                KAFKA.getBootstrapServers()
        );

        consumerProperties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "reservation-publisher-test-"
                        + UUID.randomUUID()
        );

        consumerProperties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        consumerProperties.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
        );

        consumerProperties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        consumerProperties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        return new KafkaConsumer<>(
                consumerProperties
        );
    }

    private ConsumerRecord<String, String> awaitRecord(
            KafkaConsumer<String, String> consumer
    ) {

        Instant deadline =
                Instant.now()
                        .plusSeconds(10);

        while (
                Instant.now()
                        .isBefore(deadline)
        ) {

            ConsumerRecords<String, String> records =
                    consumer.poll(
                            Duration.ofMillis(500)
                    );

            if (!records.isEmpty()) {
                return records.iterator()
                        .next();
            }
        }

        throw new AssertionError(
                "No Kafka record received within 10 seconds"
        );
    }

    private void createTopic() {

        Properties properties =
                new Properties();

        properties.put(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                KAFKA.getBootstrapServers()
        );

        try (
                AdminClient adminClient =
                        AdminClient.create(
                                properties
                        )
        ) {

            NewTopic topic =
                    new NewTopic(
                            TOPIC,
                            1,
                            (short) 1
                    );

            try {
                adminClient
                        .createTopics(
                                List.of(topic)
                        )
                        .all()
                        .get(
                                10,
                                TimeUnit.SECONDS
                        );
            } catch (Exception ignored) {
                // Topic may already exist between test methods.
            }
        }
    }
}