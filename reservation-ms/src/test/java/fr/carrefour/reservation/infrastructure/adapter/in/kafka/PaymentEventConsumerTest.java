package fr.carrefour.reservation.infrastructure.adapter.in.kafka;

import fr.carrefour.reservation.application.port.in.CancelReservationUseCase;
import fr.carrefour.reservation.application.port.in.ConfirmReservationUseCase;
import fr.carrefour.reservation.infrastructure.adapter.in.kafka.exception.InvalidPaymentEventException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Mock
    private ConfirmReservationUseCase
            confirmReservationUseCase;

    @Mock
    private CancelReservationUseCase
            cancelReservationUseCase;

    private PaymentEventConsumer consumer;

    @BeforeEach
    void setUp() {

        ObjectMapper objectMapper =
                JsonMapper.builder()
                        .findAndAddModules()
                        .build();

        consumer =
                new PaymentEventConsumer(
                        confirmReservationUseCase,
                        cancelReservationUseCase,
                        objectMapper
                );
    }

    @Test
    void shouldConfirmReservationWhenPaymentSucceeded() {

        UUID reservationId =
                UUID.randomUUID();

        AtomicBoolean executed =
                new AtomicBoolean(false);

        when(
                confirmReservationUseCase
                        .confirm(reservationId)
        ).thenReturn(
                Mono.defer(() -> {
                    executed.set(true);
                    return Mono.empty();
                })
        );

        consumer.consume(
                payload(
                        reservationId,
                        "SUCCEEDED"
                )
        );

        verify(
                confirmReservationUseCase
        ).confirm(
                reservationId
        );

        verifyNoInteractions(
                cancelReservationUseCase
        );

        assertThat(
                executed.get()
        ).isTrue();
    }

    @Test
    void shouldCancelReservationWhenPaymentFailed() {

        UUID reservationId =
                UUID.randomUUID();

        AtomicBoolean executed =
                new AtomicBoolean(false);

        when(
                cancelReservationUseCase
                        .cancel(reservationId)
        ).thenReturn(
                Mono.defer(() -> {
                    executed.set(true);
                    return Mono.empty();
                })
        );

        consumer.consume(
                payload(
                        reservationId,
                        "FAILED"
                )
        );

        verify(
                cancelReservationUseCase
        ).cancel(
                reservationId
        );

        verifyNoInteractions(
                confirmReservationUseCase
        );

        assertThat(
                executed.get()
        ).isTrue();
    }

    @Test
    void shouldRejectInvalidJson() {

        String payload =
                """
                {
                    invalid-json
                }
                """;

        assertThatThrownBy(
                () ->
                        consumer.consume(
                                payload
                        )
        )
                .isInstanceOf(
                        InvalidPaymentEventException.class
                )
                .hasMessage(
                        "Invalid payment event payload"
                );

        verifyNoInteractions(
                confirmReservationUseCase,
                cancelReservationUseCase
        );
    }

    @Test
    void shouldRejectUnknownPaymentStatus() {

        UUID reservationId =
                UUID.randomUUID();

        assertThatThrownBy(
                () ->
                        consumer.consume(
                                payload(
                                        reservationId,
                                        "UNKNOWN"
                                )
                        )
        )
                .isInstanceOf(
                        InvalidPaymentEventException.class
                )
                .hasMessage(
                        "Invalid payment event payload"
                );

        verifyNoInteractions(
                confirmReservationUseCase,
                cancelReservationUseCase
        );
    }

    @Test
    void shouldPropagateConfirmationFailure() {

        UUID reservationId =
                UUID.randomUUID();

        RuntimeException failure =
                new RuntimeException(
                        "Confirmation failed"
                );

        when(
                confirmReservationUseCase
                        .confirm(reservationId)
        ).thenReturn(
                Mono.error(failure)
        );

        assertThatThrownBy(
                () ->
                        consumer.consume(
                                payload(
                                        reservationId,
                                        "SUCCEEDED"
                                )
                        )
        )
                .isSameAs(
                        failure
                );

        verify(
                confirmReservationUseCase
        ).confirm(
                reservationId
        );

        verifyNoInteractions(
                cancelReservationUseCase
        );
    }

    @Test
    void shouldPropagateCancellationFailure() {

        UUID reservationId =
                UUID.randomUUID();

        RuntimeException failure =
                new RuntimeException(
                        "Cancellation failed"
                );

        when(
                cancelReservationUseCase
                        .cancel(reservationId)
        ).thenReturn(
                Mono.error(failure)
        );

        assertThatThrownBy(
                () ->
                        consumer.consume(
                                payload(
                                        reservationId,
                                        "FAILED"
                                )
                        )
        )
                .isSameAs(
                        failure
                );

        verify(
                cancelReservationUseCase
        ).cancel(
                reservationId
        );

        verifyNoInteractions(
                confirmReservationUseCase
        );
    }

    private String payload(
            UUID reservationId,
            String status
    ) {

        return """
                {
                  "eventId": "%s",
                  "paymentId": "%s",
                  "reservationId": "%s",
                  "status": "%s",
                  "occurredAt": "2026-09-10T10:00:00Z"
                }
                """.formatted(
                UUID.randomUUID(),
                UUID.randomUUID(),
                reservationId,
                status
        );
    }
}