package fr.carrefour.event.application.service;

import fr.carrefour.event.application.model.ReservationLifecycleStatus;
import fr.carrefour.event.application.port.out.EventAvailabilityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventAvailabilityProjectionServiceTest {

    @Mock
    private EventAvailabilityRepository availabilityRepository;

    @InjectMocks
    private EventAvailabilityProjectionService service;

    @Test
    void shouldBlockSeatWhenReservationIsHeld() {

        UUID eventId = UUID.randomUUID();
        String seatId = "A-01-01";

        when(
                availabilityRepository.blockSeat(
                        eventId,
                        seatId
                )
        ).thenReturn(
                Mono.empty()
        );

        StepVerifier
                .create(
                        service.updateAvailability(
                                eventId,
                                seatId,
                                ReservationLifecycleStatus.HELD
                        )
                )
                .verifyComplete();

        verify(
                availabilityRepository
        ).blockSeat(
                eventId,
                seatId
        );

        verify(
                availabilityRepository,
                never()
        ).releaseSeat(
                any(UUID.class),
                anyString()
        );

        verifyNoMoreInteractions(
                availabilityRepository
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = ReservationLifecycleStatus.class,
            names = {
                    "EXPIRED",
                    "CANCELLED"
            }
    )
    void shouldReleaseSeatWhenReservationNoLongerBlocksAvailability(
            ReservationLifecycleStatus status
    ) {

        UUID eventId = UUID.randomUUID();
        String seatId = "A-01-01";

        when(
                availabilityRepository.releaseSeat(
                        eventId,
                        seatId
                )
        ).thenReturn(
                Mono.empty()
        );

        StepVerifier
                .create(
                        service.updateAvailability(
                                eventId,
                                seatId,
                                status
                        )
                )
                .verifyComplete();

        verify(
                availabilityRepository
        ).releaseSeat(
                eventId,
                seatId
        );

        verify(
                availabilityRepository,
                never()
        ).blockSeat(
                any(UUID.class),
                anyString()
        );

        verifyNoMoreInteractions(
                availabilityRepository
        );
    }

    @Test
    void shouldNotChangeAvailabilityWhenReservationIsConfirmed() {

        UUID eventId = UUID.randomUUID();
        String seatId = "A-01-01";

        StepVerifier
                .create(
                        service.updateAvailability(
                                eventId,
                                seatId,
                                ReservationLifecycleStatus.CONFIRMED
                        )
                )
                .verifyComplete();

        verifyNoInteractions(
                availabilityRepository
        );
    }

    @Test
    void shouldPropagateFailureWhenBlockingSeatFails() {

        UUID eventId = UUID.randomUUID();
        String seatId = "A-01-01";

        RuntimeException failure =
                new RuntimeException(
                        "Unable to block seat"
                );

        when(
                availabilityRepository.blockSeat(
                        eventId,
                        seatId
                )
        ).thenReturn(
                Mono.error(failure)
        );

        StepVerifier
                .create(
                        service.updateAvailability(
                                eventId,
                                seatId,
                                ReservationLifecycleStatus.HELD
                        )
                )
                .expectErrorMatches(
                        exception ->
                                exception == failure
                )
                .verify();

        verify(
                availabilityRepository
        ).blockSeat(
                eventId,
                seatId
        );
    }

    @Test
    void shouldPropagateFailureWhenReleasingSeatFails() {

        UUID eventId = UUID.randomUUID();
        String seatId = "A-01-01";

        RuntimeException failure =
                new RuntimeException(
                        "Unable to release seat"
                );

        when(
                availabilityRepository.releaseSeat(
                        eventId,
                        seatId
                )
        ).thenReturn(
                Mono.error(failure)
        );

        StepVerifier
                .create(
                        service.updateAvailability(
                                eventId,
                                seatId,
                                ReservationLifecycleStatus.EXPIRED
                        )
                )
                .expectErrorMatches(
                        exception ->
                                exception == failure
                )
                .verify();

        verify(
                availabilityRepository
        ).releaseSeat(
                eventId,
                seatId
        );
    }
}