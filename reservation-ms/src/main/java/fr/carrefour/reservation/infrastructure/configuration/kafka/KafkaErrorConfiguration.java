package fr.carrefour.reservation.infrastructure.configuration.kafka;

import fr.carrefour.reservation.domain.exception.InvalidReservationStateException;
import fr.carrefour.reservation.domain.exception.ReservationNotFoundException;
import fr.carrefour.reservation.infrastructure.adapter.in.kafka.exception.InvalidPaymentEventException;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorConfiguration {

    private static final long RETRY_INTERVAL_MS = 1_000L;
    private static final long MAX_RETRIES = 3L;

    @Bean
    DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<Object, Object> kafkaTemplate
    ) {

        return new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) ->
                        new TopicPartition(
                                record.topic() + "-dlt",
                                -1
                        )
        );
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler(
            DeadLetterPublishingRecoverer recoverer
    ) {

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(
                        RETRY_INTERVAL_MS,
                        MAX_RETRIES
                )
        );

        errorHandler.addNotRetryableExceptions(
                InvalidPaymentEventException.class,
                ReservationNotFoundException.class,
                InvalidReservationStateException.class
        );

        return errorHandler;
    }
}
