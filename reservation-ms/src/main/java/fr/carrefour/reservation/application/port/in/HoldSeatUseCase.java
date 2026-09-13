package fr.carrefour.reservation.application.port.in;

import fr.carrefour.reservation.domain.model.Reservation;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface HoldSeatUseCase {

    Mono<Reservation> hold(
            UUID eventId,
            String customerId
    );
}