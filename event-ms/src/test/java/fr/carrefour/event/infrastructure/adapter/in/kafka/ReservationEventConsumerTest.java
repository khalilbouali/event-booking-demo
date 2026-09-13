package fr.carrefour.event.infrastructure.adapter.in.kafka;

import fr.carrefour.event.application.model.ReservationLifecycleStatus;
import fr.carrefour.event.application.port.in.UpdateEventAvailabilityUseCase;
import fr.carrefour.event.infrastructure.adapter.in.kafka.dto.ReservationEventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static java.util.Locale.ROOT;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationEventConsumerTest {

    @Mock
    private UpdateEventAvailabilityUseCase
            updateEventAvailabilityUseCase;

    @Mock
    private ObjectMapper objectMapper;

    private ReservationEventConsumer consumer;

    @BeforeEach
    void setUp() {

        consumer =
                new ReservationEventConsumer(
                        updateEventAvailabilityUseCase,
                        objectMapper
                );
    }

    @ParameterizedTest
    @EnumSource(ReservationLifecycleStatus.class)
    void shouldConsumeReservationEventAndUpdateAvailability(
            ReservationLifecycleStatus status
    ) throws JacksonException {

        UUID eventId =
                UUID.randomUUID();

        String seatId =
                "A-01-01";

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
                        status.name()
                                .toLowerCase(ROOT)
                );

        ReservationEventMessage event =
                mock(
                        ReservationEventMessage.class
                );

        when(
                event.eventId()
        ).thenReturn(
                eventId
        );

        when(
                event.seatId()
        ).thenReturn(
                seatId
        );

        when(
                event.status()
        ).thenReturn(
                status.name()
                        .toLowerCase(ROOT)
        );

        when(
                objectMapper.readValue(
                        payload,
                        ReservationEventMessage.class
                )
        ).thenReturn(
                event
        );

        when(
                updateEventAvailabilityUseCase
                        .updateAvailability(
                                eventId,
                                seatId,
                                status
                        )
        ).thenReturn(
                Mono.empty()
        );

        consumer.consume(
                payload
        );

        verify(
                objectMapper
        ).readValue(
                payload,
                ReservationEventMessage.class
        );

        verify(
                updateEventAvailabilityUseCase
        ).updateAvailability(
                eventId,
                seatId,
                status
        );
    }

    @Test
    void shouldConvertStatusCaseInsensitively()
            throws JacksonException {

        UUID eventId =
                UUID.randomUUID();

        String seatId =
                "A-01-01";

        String payload =
                """
                {
                  "eventId": "%s",
                  "seatId": "%s",
                  "status": "hElD"
                }
                """.formatted(
                        eventId,
                        seatId
                );

        ReservationEventMessage event =
                mock(
                        ReservationEventMessage.class
                );

        when(
                event.eventId()
        ).thenReturn(
                eventId
        );

        when(
                event.seatId()
        ).thenReturn(
                seatId
        );

        when(
                event.status()
        ).thenReturn(
                "hElD"
        );

        when(
                objectMapper.readValue(
                        payload,
                        ReservationEventMessage.class
                )
        ).thenReturn(
                event
        );

        when(
                updateEventAvailabilityUseCase
                        .updateAvailability(
                                eventId,
                                seatId,
                                ReservationLifecycleStatus.HELD
                        )
        ).thenReturn(
                Mono.empty()
        );

        consumer.consume(
                payload
        );

        verify(
                updateEventAvailabilityUseCase
        ).updateAvailability(
                eventId,
                seatId,
                ReservationLifecycleStatus.HELD
        );
    }

    @Test
    void shouldRejectInvalidJsonPayload()
            throws JacksonException {

        String payload =
                "{ invalid-json }";

        JacksonException jacksonException =
                mock(
                        JacksonException.class
                );

        when(
                objectMapper.readValue(
                        payload,
                        ReservationEventMessage.class
                )
        ).thenThrow(
                jacksonException
        );

        assertThatThrownBy(
                () -> consumer.consume(
                        payload
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Invalid reservation event payload"
                )
                .hasCause(
                        jacksonException
                );

        verifyNoInteractions(
                updateEventAvailabilityUseCase
        );
    }

    @Test
    void shouldRejectUnknownReservationStatus()
            throws JacksonException {

        UUID eventId =
                UUID.randomUUID();

        String payload =
                """
                {
                  "eventId": "%s",
                  "seatId": "A-01-01",
                  "status": "UNKNOWN"
                }
                """.formatted(
                        eventId
                );

        ReservationEventMessage event =
                mock(
                        ReservationEventMessage.class
                );

        when(
                event.status()
        ).thenReturn(
                "UNKNOWN"
        );

        when(
                objectMapper.readValue(
                        payload,
                        ReservationEventMessage.class
                )
        ).thenReturn(
                event
        );

        assertThatThrownBy(
                () -> consumer.consume(
                        payload
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                );

        verifyNoInteractions(
                updateEventAvailabilityUseCase
        );
    }

    @Test
    void shouldPropagateAvailabilityUpdateFailure()
            throws JacksonException {

        UUID eventId =
                UUID.randomUUID();

        String seatId =
                "A-01-01";

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

        ReservationEventMessage event =
                mock(
                        ReservationEventMessage.class
                );

        RuntimeException failure =
                new RuntimeException(
                        "Mongo unavailable"
                );

        when(
                event.eventId()
        ).thenReturn(
                eventId
        );

        when(
                event.seatId()
        ).thenReturn(
                seatId
        );

        when(
                event.status()
        ).thenReturn(
                "HELD"
        );

        when(
                objectMapper.readValue(
                        payload,
                        ReservationEventMessage.class
                )
        ).thenReturn(
                event
        );

        when(
                updateEventAvailabilityUseCase
                        .updateAvailability(
                                eventId,
                                seatId,
                                ReservationLifecycleStatus.HELD
                        )
        ).thenReturn(
                Mono.error(
                        failure
                )
        );

        assertThatThrownBy(
                () -> consumer.consume(
                        payload
                )
        )
                .isSameAs(
                        failure
                );

        verify(
                updateEventAvailabilityUseCase
        ).updateAvailability(
                eventId,
                seatId,
                ReservationLifecycleStatus.HELD
        );
    }
}