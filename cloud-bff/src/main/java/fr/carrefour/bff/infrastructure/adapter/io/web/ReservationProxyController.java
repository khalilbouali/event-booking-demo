package fr.carrefour.bff.infrastructure.adapter.io.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static fr.carrefour.bff.infrastructure.adapter.io.web.ReservationViewResponse.from;
import static reactor.core.publisher.Flux.empty;

@RestController
public class ReservationProxyController {

    private final ProxyForwarder proxyForwarder;
    private final String reservationServiceUrl;
    private final String eventServiceUrl;
    private final WebClient microserviceWebClient;

    public ReservationProxyController(
            ProxyForwarder proxyForwarder,
            @Value("${services.reservation.base-url}") String reservationServiceUrl,
            @Value("${services.event.base-url}") String eventServiceUrl,
            WebClient microserviceWebClient
    ) {
        this.proxyForwarder = proxyForwarder;
        this.reservationServiceUrl = reservationServiceUrl;
        this.eventServiceUrl = eventServiceUrl;
        this.microserviceWebClient = microserviceWebClient;
    }

    @RequestMapping("/api/reservations/**")
    Mono<ResponseEntity<byte[]>> proxy(
            ServerWebExchange exchange
    ) {
        return proxyForwarder.forward(
                exchange,
                reservationServiceUrl
        );
    }

    @GetMapping("/api/reservations")
    public Flux<ReservationViewResponse> findMine() {

        return microserviceWebClient
                .get()
                .uri(
                        reservationServiceUrl
                                + "/api/reservations"
                )
                .retrieve()
                .bodyToFlux(ReservationResponse.class)
                .collectList()
                .flatMapMany(reservations -> {

                    if (reservations.isEmpty()) {
                        return empty();
                    }

                    List<UUID> eventIds =
                            reservations.stream()
                                    .map(ReservationResponse::eventId)
                                    .distinct()
                                    .toList();

                    URI eventDetailsUri =
                            UriComponentsBuilder
                                    .fromUriString(
                                            eventServiceUrl
                                                    + "/internal/events/details"
                                    )
                                    .queryParam(
                                            "ids",
                                            eventIds
                                    )
                                    .build()
                                    .toUri();

                    return microserviceWebClient
                            .get()
                            .uri(eventDetailsUri)
                            .retrieve()
                            .bodyToFlux(
                                    EventDetailsResponse.class
                            )
                            .collectMap(
                                    EventDetailsResponse::id
                            )
                            .flatMapMany(eventsById ->
                                    Flux.fromIterable(
                                                    reservations
                                            )
                                            .map(reservation ->
                                                    from(
                                                            reservation,
                                                            eventsById.get(
                                                                    reservation.eventId()
                                                            )
                                                    )
                                            )
                            );
                });
    }
}
