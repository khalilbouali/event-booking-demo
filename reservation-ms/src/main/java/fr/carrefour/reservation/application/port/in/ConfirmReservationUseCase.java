package fr.carrefour.reservation.application.port.in;

import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ConfirmReservationUseCase {

    Mono<Void> confirm(UUID reservationId);
}
