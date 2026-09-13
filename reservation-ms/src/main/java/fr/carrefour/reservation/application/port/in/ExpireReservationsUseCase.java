package fr.carrefour.reservation.application.port.in;

import reactor.core.publisher.Mono;

public interface ExpireReservationsUseCase {

    Mono<Long> expireDueReservations();
}