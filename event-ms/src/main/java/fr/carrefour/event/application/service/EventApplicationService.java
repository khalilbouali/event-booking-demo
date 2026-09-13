package fr.carrefour.event.application.service;

import fr.carrefour.event.application.model.EventDetails;
import fr.carrefour.event.application.model.EventSummary;
import fr.carrefour.event.application.port.in.CreateEventUseCase;
import fr.carrefour.event.application.port.in.GetEventUseCase;
import fr.carrefour.event.application.port.in.GetEventsByIdsUseCase;
import fr.carrefour.event.application.port.in.ListEventsUseCase;
import fr.carrefour.event.application.port.out.EventAvailabilityRepository;
import fr.carrefour.event.application.port.out.EventRepository;
import fr.carrefour.event.domain.exception.EventNotFoundException;
import fr.carrefour.event.domain.model.Event;
import fr.carrefour.event.domain.model.Seat;
import fr.carrefour.event.domain.service.SeatLayoutGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static fr.carrefour.event.domain.model.Event.create;
import static reactor.core.publisher.Mono.defer;
import static reactor.core.publisher.Mono.error;

@Service
@RequiredArgsConstructor
public class EventApplicationService
        implements CreateEventUseCase, GetEventUseCase, ListEventsUseCase, GetEventsByIdsUseCase {

    private final EventRepository eventRepository;
    private final EventAvailabilityRepository availabilityRepository;
    private final SeatLayoutGenerator seatLayoutGenerator;

    @Override
    public Mono<EventSummary> createEvent(
            String name,
            String venue,
            Instant startsAt,
            int seatCount,
            BigDecimal price
    ) {

        List<Seat> seats =
                seatLayoutGenerator.generate(seatCount);

        Event event = create(
                name,
                venue,
                startsAt,
                seats,
                price
        );

        return eventRepository
                .save(event)
                .flatMap(savedEvent ->
                        availabilityRepository
                                .initialize(
                                        savedEvent.getId(),
                                        savedEvent.getSeats().size()
                                )
                                .thenReturn(savedEvent)
                )
                .flatMap(this::toSummary);
    }

    @Override
    public Mono<EventSummary> getById(UUID eventId) {
        return eventRepository.findById(eventId)
                .switchIfEmpty(
                        error(
                                new EventNotFoundException(eventId)
                        )
                )
                .flatMap(this::toSummary);
    }

    @Override
    public Flux<EventSummary> findAll() {

        return eventRepository
                .findAll()
                .flatMap(this::toSummary);
    }

    @Override
    public Flux<EventDetails> getByIds(
            Collection<UUID> eventIds
    ) {
        return eventRepository
                .findAllById(eventIds)
                .map(event ->
                        new EventDetails(
                                event.getId(),
                                event.getName(),
                                event.getVenue(),
                                event.getStartsAt()
                        )
                );
    }

    private Mono<EventSummary> toSummary(
            Event event
    ) {

        int totalSeats =
                event.getSeats().size();

        return availabilityRepository
                .findByEventId(event.getId())

                .switchIfEmpty(
                        defer(() ->
                                availabilityRepository
                                        .initialize(
                                                event.getId(),
                                                totalSeats
                                        )
                                        .then(
                                                availabilityRepository
                                                        .findByEventId(
                                                                event.getId()
                                                        )
                                        )
                        )
                )

                .switchIfEmpty(
                        error(
                                new IllegalStateException(
                                        "Availability projection could not be initialized for event "
                                                + event.getId()
                                )
                        )
                )

                .map(availability ->
                        new EventSummary(
                                event.getId(),
                                event.getName(),
                                event.getVenue(),
                                event.getStartsAt(),
                                totalSeats,
                                availability.remainingSeats(),
                                event.getPrice()
                        )
                );
    }
}
