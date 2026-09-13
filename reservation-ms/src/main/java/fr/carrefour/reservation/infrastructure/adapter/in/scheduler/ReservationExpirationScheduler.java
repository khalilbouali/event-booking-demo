package fr.carrefour.reservation.infrastructure.adapter.in.scheduler;

import fr.carrefour.reservation.application.port.in.ExpireReservationsUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static org.slf4j.LoggerFactory.getLogger;


@Component
@RequiredArgsConstructor
public class ReservationExpirationScheduler {

    private static final Logger log =
            getLogger(
                    ReservationExpirationScheduler.class
            );

    private final ExpireReservationsUseCase
            expireReservationsUseCase;

    @Scheduled(
            fixedDelayString =
                    "${reservation.expiration-check-delay:5000}"
    )
    public void expireReservations() {

        expireReservationsUseCase
                .expireDueReservations()
                .subscribe(
                        count ->
                                log.debug(
                                        "{} reservation(s) expired",
                                        count
                                ),
                        exception ->
                                log.error(
                                        "Failed to expire reservations",
                                        exception
                                )
                );
    }
}