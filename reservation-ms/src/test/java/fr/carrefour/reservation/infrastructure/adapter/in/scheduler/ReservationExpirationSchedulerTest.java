package fr.carrefour.reservation.infrastructure.adapter.in.scheduler;

import fr.carrefour.reservation.application.port.in.ExpireReservationsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationExpirationSchedulerTest {

    @Mock
    private ExpireReservationsUseCase
            expireReservationsUseCase;

    @InjectMocks
    private ReservationExpirationScheduler scheduler;

    @Test
    void shouldTriggerExpirationOfDueReservations() {

        when(
                expireReservationsUseCase
                        .expireDueReservations()
        ).thenReturn(
                Mono.just(3L)
        );

        scheduler.expireReservations();

        verify(
                expireReservationsUseCase
        ).expireDueReservations();
    }

    @Test
    void shouldSubscribeToExpirationProcess() {

        AtomicBoolean subscribed =
                new AtomicBoolean(false);

        when(
                expireReservationsUseCase
                        .expireDueReservations()
        ).thenReturn(
                Mono.defer(() -> {

                    subscribed.set(true);

                    return Mono.just(2L);
                })
        );

        scheduler.expireReservations();

        assertThat(
                subscribed.get()
        ).isTrue();
    }
}