package fr.carrefour.reservation.infrastructure.adapter.in.kafka;

import fr.carrefour.reservation.application.port.in.CancelReservationUseCase;
import fr.carrefour.reservation.application.port.in.ConfirmReservationUseCase;
import fr.carrefour.reservation.infrastructure.adapter.in.kafka.event.PaymentEvent;
import fr.carrefour.reservation.infrastructure.adapter.in.kafka.event.PaymentStatus;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringJUnitConfig(PaymentEventConsumerIT.TestConfiguration.class)
@TestPropertySource(properties = {
        "kafka.topics.payment-events=payment-events-test",
        "spring.kafka.consumer.group-id=reservation-payment-test"
})
class PaymentEventConsumerIT {

    private static final String TOPIC =
            "payment-events-test";

    private static final UUID PAYMENT_ID =
            UUID.fromString(
                    "11111111-1111-1111-1111-111111111111"
            );

    private static final UUID RESERVATION_ID =
            UUID.fromString(
                    "22222222-2222-2222-2222-222222222222"
            );

    private static final Instant OCCURRED_AT =
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

    @jakarta.annotation.Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    @jakarta.annotation.Resource
    private ObjectMapper objectMapper;

    @jakarta.annotation.Resource
    private ConfirmReservationUseCase confirmReservationUseCase;

    @jakarta.annotation.Resource
    private CancelReservationUseCase cancelReservationUseCase;

    @BeforeEach
    void setUp() {

        reset(
                confirmReservationUseCase,
                cancelReservationUseCase
        );

        when(
                confirmReservationUseCase.confirm(any())
        ).thenReturn(
                Mono.empty()
        );

        when(
                cancelReservationUseCase.cancel(any())
        ).thenReturn(
                Mono.empty()
        );
    }

    @Test
    void shouldConfirmReservationWhenPaymentSucceeded()
            throws Exception {

        PaymentEvent event =
                new PaymentEvent(
                        UUID.randomUUID(),
                        PAYMENT_ID,
                        RESERVATION_ID,
                        PaymentStatus.SUCCEEDED,
                        OCCURRED_AT
                );

        kafkaTemplate
                .send(
                        TOPIC,
                        RESERVATION_ID.toString(),
                        objectMapper.writeValueAsString(event)
                )
                .get(
                        10,
                        TimeUnit.SECONDS
                );

        verify(
                confirmReservationUseCase,
                timeout(10_000)
        ).confirm(
                eq(RESERVATION_ID)
        );

        verify(
                cancelReservationUseCase,
                after(500).never()
        ).cancel(
                any()
        );
    }

    @Test
    void shouldCancelReservationWhenPaymentFailed()
            throws Exception {

        PaymentEvent event =
                new PaymentEvent(
                        UUID.randomUUID(),
                        PAYMENT_ID,
                        RESERVATION_ID,
                        PaymentStatus.FAILED,
                        OCCURRED_AT
                );

        kafkaTemplate
                .send(
                        TOPIC,
                        RESERVATION_ID.toString(),
                        objectMapper.writeValueAsString(event)
                )
                .get(
                        10,
                        TimeUnit.SECONDS
                );

        verify(
                cancelReservationUseCase,
                timeout(10_000)
        ).cancel(
                eq(RESERVATION_ID)
        );

        verify(
                confirmReservationUseCase,
                after(500).never()
        ).confirm(
                any()
        );
    }

    @Configuration
    @EnableKafka
    @Import(PaymentEventConsumer.class)
    static class TestConfiguration {

        @Bean
        ConfirmReservationUseCase confirmReservationUseCase() {
            return mock(
                    ConfirmReservationUseCase.class
            );
        }

        @Bean
        CancelReservationUseCase cancelReservationUseCase() {
            return mock(
                    CancelReservationUseCase.class
            );
        }

        @Bean
        ObjectMapper objectMapper() {
            return JsonMapper.builder()
                    .findAndAddModules()
                    .build();
        }

        @Bean
        ProducerFactory<String, String> producerFactory() {

            Map<String, Object> properties =
                    new HashMap<>();

            properties.put(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    KAFKA.getBootstrapServers()
            );

            properties.put(
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                    StringSerializer.class
            );

            properties.put(
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                    StringSerializer.class
            );

            properties.put(
                    ProducerConfig.ACKS_CONFIG,
                    "all"
            );

            return new DefaultKafkaProducerFactory<>(
                    properties
            );
        }

        @Bean
        KafkaTemplate<String, String> kafkaTemplate(
                ProducerFactory<String, String> producerFactory
        ) {

            return new KafkaTemplate<>(
                    producerFactory
            );
        }

        @Bean
        ConsumerFactory<String, String> consumerFactory() {

            Map<String, Object> properties =
                    new HashMap<>();

            properties.put(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    KAFKA.getBootstrapServers()
            );

            properties.put(
                    ConsumerConfig.GROUP_ID_CONFIG,
                    "reservation-payment-test"
            );

            properties.put(
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                    "earliest"
            );

            properties.put(
                    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                    false
            );

            properties.put(
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                    StringDeserializer.class
            );

            properties.put(
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                    StringDeserializer.class
            );

            return new DefaultKafkaConsumerFactory<>(
                    properties
            );
        }

        @Bean
        ConcurrentKafkaListenerContainerFactory<String, String>
        kafkaListenerContainerFactory(
                ConsumerFactory<String, String> consumerFactory
        ) {

            ConcurrentKafkaListenerContainerFactory<String, String>
                    factory =
                    new ConcurrentKafkaListenerContainerFactory<>();

            factory.setConsumerFactory(
                    consumerFactory
            );

            return factory;
        }

        @Bean
        KafkaAdmin kafkaAdmin() {

            Map<String, Object> properties =
                    Map.of(
                            AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                            KAFKA.getBootstrapServers()
                    );

            return new KafkaAdmin(
                    properties
            );
        }

        @Bean
        NewTopic paymentEventsTopic() {

            return new NewTopic(
                    TOPIC,
                    1,
                    (short) 1
            );
        }
    }
}