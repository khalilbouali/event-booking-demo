package fr.carrefour.event.application.service;

import fr.carrefour.event.application.model.ReservationLifecycleStatus;
import fr.carrefour.event.application.port.in.UpdateEventAvailabilityUseCase;
import fr.carrefour.event.application.port.out.EventAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static reactor.core.publisher.Mono.empty;

@Service
@RequiredArgsConstructor
public class EventAvailabilityProjectionService
        implements UpdateEventAvailabilityUseCase {

    private final EventAvailabilityRepository availabilityRepository;

    @Override
    public Mono<Void> updateAvailability(
            UUID eventId,
            String seatId,
            ReservationLifecycleStatus status
    ) {

        return switch (status) {

            case HELD ->
                    availabilityRepository.blockSeat(
                            eventId,
                            seatId
                    );

            case EXPIRED, CANCELLED ->
                    availabilityRepository.releaseSeat(
                            eventId,
                            seatId
                    );

            case CONFIRMED ->
                    empty();
        };
    }
}
