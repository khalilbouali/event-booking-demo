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
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static fr.carrefour.bff.infrastructure.adapter.io.web.PaymentViewResponse.from;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;
import static reactor.core.publisher.Flux.empty;
import static reactor.core.publisher.Flux.fromIterable;

@RestController
public class PaymentProxyController {

    private final ProxyForwarder proxyForwarder;
    private final WebClient microserviceWebClient;

    private final String paymentServiceUrl;
    private final String reservationServiceUrl;
    private final String eventServiceUrl;

    public PaymentProxyController(
            ProxyForwarder proxyForwarder,
            WebClient microserviceWebClient,

            @Value("${services.payment.base-url}")
            String paymentServiceUrl,

            @Value("${services.reservation.base-url}")
            String reservationServiceUrl,

            @Value("${services.event.base-url}")
            String eventServiceUrl
    ) {
        this.proxyForwarder = proxyForwarder;
        this.microserviceWebClient =
                microserviceWebClient;

        this.paymentServiceUrl =
                paymentServiceUrl;

        this.reservationServiceUrl =
                reservationServiceUrl;

        this.eventServiceUrl =
                eventServiceUrl;
    }

    @RequestMapping("/api/payments/**")
    Mono<ResponseEntity<byte[]>> proxy(
            ServerWebExchange exchange
    ) {
        return proxyForwarder.forward(
                exchange,
                paymentServiceUrl
        );
    }

    @GetMapping("/api/payments")
    public Flux<PaymentViewResponse> findMine() {

        return microserviceWebClient
                .get()
                .uri(
                        paymentServiceUrl
                                + "/api/payments"
                )
                .retrieve()
                .bodyToFlux(PaymentResponse.class)
                .collectList()

                .flatMapMany(payments -> {

                    if (payments.isEmpty()) {
                        return empty();
                    }

                    return loadReservations()
                            .flatMapMany(reservationsById ->
                                    loadPaymentViews(
                                            payments,
                                            reservationsById
                                    )
                            );
                });
    }

    private Mono<Map<UUID, ReservationResponse>> loadReservations() {

        return microserviceWebClient
                .get()
                .uri(
                        reservationServiceUrl
                                + "/api/reservations"
                )
                .retrieve()
                .bodyToFlux(ReservationResponse.class)
                .collectMap(
                        ReservationResponse::id
                );
    }

    private Flux<PaymentViewResponse> loadPaymentViews(
            List<PaymentResponse> payments,
            Map<UUID, ReservationResponse> reservationsById
    ) {

        List<UUID> eventIds =
                payments.stream()
                        .map(PaymentResponse::reservationId)
                        .map(reservationsById::get)
                        .filter(Objects::nonNull)
                        .map(ReservationResponse::eventId)
                        .distinct()
                        .toList();

        if (eventIds.isEmpty()) {
            return fromIterable(payments)
                    .map(payment ->
                            from(
                                    payment,
                                    null,
                                    null
                            )
                    );
        }

        return loadEvents(eventIds)
                .flatMapMany(eventsById ->
                        fromIterable(payments)
                                .map(payment -> {

                                    ReservationResponse reservation =
                                            reservationsById.get(
                                                    payment.reservationId()
                                            );

                                    EventDetailsResponse event =
                                            reservation != null
                                                    ? eventsById.get(
                                                    reservation.eventId()
                                            )
                                                    : null;

                                    return from(
                                            payment,
                                            reservation,
                                            event
                                    );
                                })
                );
    }

    private Mono<Map<UUID, EventDetailsResponse>> loadEvents(
            List<UUID> eventIds
    ) {

        UriComponentsBuilder builder =
                fromUriString(
                                eventServiceUrl
                                        + "/internal/events/details"
                        );

        eventIds.forEach(
                eventId ->
                        builder.queryParam(
                                "ids",
                                eventId
                        )
        );

        URI uri =
                builder
                        .build()
                        .toUri();

        return microserviceWebClient
                .get()
                .uri(uri)
                .retrieve()
                .bodyToFlux(EventDetailsResponse.class)
                .collectMap(
                        EventDetailsResponse::id
                );
    }
}
