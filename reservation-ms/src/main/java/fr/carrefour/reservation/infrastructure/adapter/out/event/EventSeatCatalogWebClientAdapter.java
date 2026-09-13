package fr.carrefour.reservation.infrastructure.adapter.out.event;

import fr.carrefour.reservation.application.model.EventSeatCandidates;
import fr.carrefour.reservation.application.port.out.EventSeatCatalog;
import fr.carrefour.reservation.infrastructure.adapter.out.event.dto.EventSeatCandidatesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EventSeatCatalogWebClientAdapter
        implements EventSeatCatalog {

    private final WebClient eventWebClient;

    @Override
    public Mono<EventSeatCandidates> getSeatCandidates(
            UUID eventId,
            int limit
    ) {

        return eventWebClient
                .get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path(
                                        "/internal/events/{eventId}/seat-candidates"
                                )
                                .queryParam(
                                        "limit",
                                        limit
                                )
                                .build(eventId)
                )
                .retrieve()
                .bodyToMono(
                        EventSeatCandidatesResponse.class
                )
                .map(response ->
                        new EventSeatCandidates(
                                response.eventId(),
                                response.seatIds()
                        )
                );
    }
}
