package fr.carrefour.event.infrastructure.adapter.in.web;

import fr.carrefour.event.application.port.in.GetEventSeatCandidatesUseCase;
import fr.carrefour.event.application.port.in.GetEventsByIdsUseCase;
import fr.carrefour.event.infrastructure.adapter.in.web.dto.EventDetailsResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final GetEventSeatCandidatesUseCase
            getEventSeatCandidatesUseCase;
    private final GetEventsByIdsUseCase  getEventsByIdsUseCase;

    @GetMapping("/{eventId}/seat-candidates")
    public Mono<EventSeatCandidatesResponse> getSeatCandidates(

            @PathVariable
            UUID eventId,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            int limit
    ) {

        return getEventSeatCandidatesUseCase
                .getSeatCandidates(
                        eventId,
                        limit
                )
                .map(candidates ->
                        new EventSeatCandidatesResponse(
                                candidates.eventId(),
                                candidates.seatIds()
                        )
                );
    }

    @GetMapping("/details")
    public Flux<EventDetailsResponse> getDetails(
            @RequestParam List<UUID> ids
    ) {
        return getEventsByIdsUseCase
                .getByIds(ids)
                .map(EventDetailsResponse::from);
    }
}
