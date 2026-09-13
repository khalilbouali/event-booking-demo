package fr.carrefour.reservation.application.port.out;

import fr.carrefour.reservation.domain.model.Reservation;
import reactor.core.publisher.Mono;

public interface ReservationEventPublisher {

    Mono<Void> publish(Reservation reservation);
}
