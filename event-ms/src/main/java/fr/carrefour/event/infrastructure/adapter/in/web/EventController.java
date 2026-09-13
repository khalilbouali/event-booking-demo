package fr.carrefour.event.infrastructure.adapter.in.web;

import fr.carrefour.event.application.port.in.CreateEventUseCase;
import fr.carrefour.event.application.port.in.GetEventUseCase;
import fr.carrefour.event.application.port.in.ListEventsUseCase;
import fr.carrefour.event.infrastructure.adapter.in.web.dto.CreateEventRequest;
import fr.carrefour.event.infrastructure.adapter.in.web.dto.EventSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final CreateEventUseCase createEventUseCase;
    private final GetEventUseCase getEventUseCase;
    private final ListEventsUseCase listEventsUseCase;

    @PostMapping
    @ResponseStatus(CREATED)
    public Mono<EventSummaryResponse> create(
            @Valid @RequestBody CreateEventRequest request
    ) {
        return createEventUseCase.createEvent(
                request.name(),
                request.venue(),
                request.startsAt(),
                request.seatCount(),
                request.price()
        ).map(EventWebMapper::toResponse);
    }

    @GetMapping("/{eventId}")
    public Mono<EventSummaryResponse> getById(
            @PathVariable UUID eventId
    ) {
        return getEventUseCase
                .getById(eventId)
                .map(EventWebMapper::toResponse);
    }

    @GetMapping
    public Flux<EventSummaryResponse> findAll() {
        return listEventsUseCase
                .findAll()
                .map(EventWebMapper::toResponse);
    }
}
