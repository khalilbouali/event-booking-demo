package fr.carrefour.event.application.service;

import fr.carrefour.event.application.model.EventSeatCandidates;
import fr.carrefour.event.application.port.in.GetEventSeatCandidatesUseCase;
import fr.carrefour.event.application.port.out.EventAvailabilityRepository;
import fr.carrefour.event.application.port.out.EventRepository;
import fr.carrefour.event.domain.exception.EventNotFoundException;
import fr.carrefour.event.domain.model.Seat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static reactor.core.publisher.Mono.error;

@Service
@RequiredArgsConstructor
public class EventSeatCatalogService
        implements GetEventSeatCandidatesUseCase {

    private final EventRepository eventRepository;
    private final EventAvailabilityRepository
            availabilityRepository;

    @Override
    public Mono<EventSeatCandidates> getSeatCandidates(
            UUID eventId,
            int limit
    ) {

        return eventRepository
                .findById(eventId)
                .switchIfEmpty(
                        error(
                                new EventNotFoundException(
                                        eventId
                                )
                        )
                )
                .flatMap(event ->
                        availabilityRepository
                                .findBlockedSeatIds(eventId)
                                .map(blockedSeatIds -> {

                                    List<String> candidates =
                                            event.getSeats()
                                                    .stream()
                                                    .map(Seat::id)
                                                    .filter(seatId ->
                                                            !blockedSeatIds
                                                                    .contains(
                                                                            seatId
                                                                    )
                                                    )
                                                    .limit(limit)
                                                    .toList();

                                    return new EventSeatCandidates(
                                            eventId,
                                            candidates
                                    );
                                })
                );
    }
}
