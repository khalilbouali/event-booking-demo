package fr.carrefour.event.application.port.in;

import fr.carrefour.event.application.model.ReservationLifecycleStatus;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UpdateEventAvailabilityUseCase {

    Mono<Void> updateAvailability(
            UUID eventId,
            String seatId,
            ReservationLifecycleStatus status
    );
}