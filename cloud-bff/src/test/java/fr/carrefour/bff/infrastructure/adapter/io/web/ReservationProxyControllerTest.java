package fr.carrefour.bff.infrastructure.adapter.io.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReservationProxyControllerTest {

    private static final String RESERVATION_SERVICE_URL =
            "http://reservation-ms:8080";

    private static final String EVENT_SERVICE_URL =
            "http://event-ms:8080";

    private ProxyForwarder proxyForwarder;

    private WebTestClient webTestClient;

    private DisposableServer server;

    @BeforeEach
    void setUp() {

        proxyForwarder =
                mock(ProxyForwarder.class);

        WebClient microserviceWebClient =
                WebClient.builder()
                        .build();

        ReservationProxyController controller =
                new ReservationProxyController(
                        proxyForwarder,
                        RESERVATION_SERVICE_URL,
                        EVENT_SERVICE_URL,
                        microserviceWebClient
                );

        webTestClient =
                WebTestClient
                        .bindToController(controller)
                        .build();
    }

    @AfterEach
    void tearDown() {

        if (server != null) {
            server.disposeNow();
        }
    }

    @Test
    void shouldForwardReservationRequestToReservationService() {

        byte[] responseBody =
                """
                {"id":"reservation-123"}
                """.getBytes();

        when(
                proxyForwarder.forward(
                        any(ServerWebExchange.class),
                        eq(RESERVATION_SERVICE_URL)
                )
        ).thenReturn(
                Mono.just(
                        ResponseEntity.ok(
                                responseBody
                        )
                )
        );

        webTestClient
                .post()
                .uri(
                        "/api/reservations/reservation-123/confirm"
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(byte[].class)
                .isEqualTo(responseBody);

        verify(
                proxyForwarder
        ).forward(
                any(ServerWebExchange.class),
                eq(RESERVATION_SERVICE_URL)
        );
    }

    @Test
    void shouldPropagateReservationServiceStatus() {

        when(
                proxyForwarder.forward(
                        any(ServerWebExchange.class),
                        eq(RESERVATION_SERVICE_URL)
                )
        ).thenReturn(
                Mono.just(
                        ResponseEntity
                                .notFound()
                                .build()
                )
        );

        webTestClient
                .delete()
                .uri(
                        "/api/reservations/reservation-123"
                )
                .exchange()
                .expectStatus()
                .isNotFound();

        verify(
                proxyForwarder
        ).forward(
                any(ServerWebExchange.class),
                eq(RESERVATION_SERVICE_URL)
        );
    }

    @Test
    void shouldReturnEmptyFluxWhenNoReservationsExist() {

        List<String> requestedUris =
                new CopyOnWriteArrayList<>();

        server =
                HttpServer.create()
                        .port(0)
                        .handle((request, response) -> {

                            requestedUris.add(
                                    request.uri()
                            );

                            return response
                                    .status(200)
                                    .header(
                                            "Content-Type",
                                            "application/json"
                                    )
                                    .sendString(
                                            Mono.just("[]")
                                    )
                                    .then();
                        })
                        .bindNow();

        String baseUrl =
                "http://localhost:"
                        + server.port();

        ReservationProxyController controller =
                new ReservationProxyController(
                        proxyForwarder,
                        baseUrl,
                        baseUrl,
                        WebClient.builder()
                                .build()
                );

        StepVerifier
                .create(
                        controller.findMine()
                )
                .verifyComplete();

        assertThat(
                requestedUris
        ).containsExactly(
                "/api/reservations"
        );

        verifyNoInteractions(
                proxyForwarder
        );
    }

    @Test
    void shouldEnrichReservationsWithEventDetails() {

        UUID reservationId =
                UUID.randomUUID();

        UUID eventId =
                UUID.randomUUID();

        Instant createdAt =
                Instant.parse(
                        "2026-09-10T08:00:00Z"
                );

        Instant expiresAt =
                Instant.parse(
                        "2026-09-10T08:15:00Z"
                );

        Instant startsAt =
                Instant.parse(
                        "2026-09-15T20:00:00Z"
                );

        server =
                HttpServer.create()
                        .port(0)
                        .handle((request, response) -> {

                            if (
                                    request.uri()
                                            .equals(
                                                    "/api/reservations"
                                            )
                            ) {

                                String body =
                                        """
                                        [
                                          {
                                            "id": "%s",
                                            "eventId": "%s",
                                            "seatId": "A-12",
                                            "customerId": "customer-123",
                                            "status": "CONFIRMED",
                                            "createdAt": "%s",
                                            "expiresAt": "%s"
                                          }
                                        ]
                                        """.formatted(
                                                reservationId,
                                                eventId,
                                                createdAt,
                                                expiresAt
                                        );

                                return response
                                        .status(200)
                                        .header(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .sendString(
                                                Mono.just(body)
                                        )
                                        .then();
                            }

                            if (
                                    request.uri()
                                            .startsWith(
                                                    "/internal/events/details"
                                            )
                            ) {

                                String body =
                                        """
                                        [
                                          {
                                            "id": "%s",
                                            "name": "Carrefour Music Festival",
                                            "venue": "Casablanca Arena",
                                            "startsAt": "%s"
                                          }
                                        ]
                                        """.formatted(
                                                eventId,
                                                startsAt
                                        );

                                return response
                                        .status(200)
                                        .header(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .sendString(
                                                Mono.just(body)
                                        )
                                        .then();
                            }

                            return response
                                    .status(404)
                                    .send();
                        })
                        .bindNow();

        String baseUrl =
                "http://localhost:"
                        + server.port();

        ReservationProxyController controller =
                new ReservationProxyController(
                        proxyForwarder,
                        baseUrl,
                        baseUrl,
                        WebClient.builder()
                                .build()
                );

        StepVerifier
                .create(
                        controller.findMine()
                )
                .assertNext(view -> {

                    assertThat(
                            view.id()
                    ).isEqualTo(
                            reservationId
                    );

                    assertThat(
                            view.eventId()
                    ).isEqualTo(
                            eventId
                    );

                    assertThat(
                            view.seatId()
                    ).isEqualTo(
                            "A-12"
                    );

                    assertThat(
                            view.status()
                    ).isEqualTo(
                            "CONFIRMED"
                    );

                    assertThat(
                            view.createdAt()
                    ).isEqualTo(
                            createdAt
                    );

                    assertThat(
                            view.expiresAt()
                    ).isEqualTo(
                            expiresAt
                    );

                    assertThat(
                            view.eventName()
                    ).isEqualTo(
                            "Carrefour Music Festival"
                    );

                    assertThat(
                            view.eventVenue()
                    ).isEqualTo(
                            "Casablanca Arena"
                    );

                    assertThat(
                            view.eventStartsAt()
                    ).isEqualTo(
                            startsAt
                    );
                })
                .verifyComplete();
    }

    @Test
    void shouldUseUnknownEventWhenEventDetailsAreMissing() {

        UUID reservationId =
                UUID.randomUUID();

        UUID eventId =
                UUID.randomUUID();

        Instant createdAt =
                Instant.parse(
                        "2026-09-10T08:00:00Z"
                );

        Instant expiresAt =
                Instant.parse(
                        "2026-09-10T08:15:00Z"
                );

        server =
                HttpServer.create()
                        .port(0)
                        .handle((request, response) -> {

                            if (
                                    request.uri()
                                            .equals(
                                                    "/api/reservations"
                                            )
                            ) {

                                String body =
                                        """
                                        [
                                          {
                                            "id": "%s",
                                            "eventId": "%s",
                                            "seatId": "B-5",
                                            "customerId": "customer-123",
                                            "status": "HELD",
                                            "createdAt": "%s",
                                            "expiresAt": "%s"
                                          }
                                        ]
                                        """.formatted(
                                                reservationId,
                                                eventId,
                                                createdAt,
                                                expiresAt
                                        );

                                return response
                                        .status(200)
                                        .header(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .sendString(
                                                Mono.just(body)
                                        )
                                        .then();
                            }

                            if (
                                    request.uri()
                                            .startsWith(
                                                    "/internal/events/details"
                                            )
                            ) {

                                return response
                                        .status(200)
                                        .header(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .sendString(
                                                Mono.just("[]")
                                        )
                                        .then();
                            }

                            return response
                                    .status(404)
                                    .send();
                        })
                        .bindNow();

        String baseUrl =
                "http://localhost:"
                        + server.port();

        ReservationProxyController controller =
                new ReservationProxyController(
                        proxyForwarder,
                        baseUrl,
                        baseUrl,
                        WebClient.builder()
                                .build()
                );

        StepVerifier
                .create(
                        controller.findMine()
                )
                .assertNext(view -> {

                    assertThat(
                            view.id()
                    ).isEqualTo(
                            reservationId
                    );

                    assertThat(
                            view.eventId()
                    ).isEqualTo(
                            eventId
                    );

                    assertThat(
                            view.seatId()
                    ).isEqualTo(
                            "B-5"
                    );

                    assertThat(
                            view.status()
                    ).isEqualTo(
                            "HELD"
                    );

                    assertThat(
                            view.eventName()
                    ).isEqualTo(
                            "Unknown event"
                    );

                    assertThat(
                            view.eventVenue()
                    ).isNull();

                    assertThat(
                            view.eventStartsAt()
                    ).isNull();
                })
                .verifyComplete();
    }

    @Test
    void shouldRequestDistinctEventIds() {

        UUID eventId =
                UUID.randomUUID();

        UUID reservation1Id =
                UUID.randomUUID();

        UUID reservation2Id =
                UUID.randomUUID();

        List<String> requestedUris =
                new CopyOnWriteArrayList<>();

        server =
                HttpServer.create()
                        .port(0)
                        .handle((request, response) -> {

                            requestedUris.add(
                                    request.uri()
                            );

                            if (
                                    request.uri()
                                            .equals(
                                                    "/api/reservations"
                                            )
                            ) {

                                String body =
                                        """
                                        [
                                          {
                                            "id": "%s",
                                            "eventId": "%s",
                                            "seatId": "A-1",
                                            "customerId": "customer-123",
                                            "status": "CONFIRMED",
                                            "createdAt": "2026-09-10T08:00:00Z",
                                            "expiresAt": "2026-09-10T08:15:00Z"
                                          },
                                          {
                                            "id": "%s",
                                            "eventId": "%s",
                                            "seatId": "A-2",
                                            "customerId": "customer-123",
                                            "status": "CONFIRMED",
                                            "createdAt": "2026-09-10T08:01:00Z",
                                            "expiresAt": "2026-09-10T08:16:00Z"
                                          }
                                        ]
                                        """.formatted(
                                                reservation1Id,
                                                eventId,
                                                reservation2Id,
                                                eventId
                                        );

                                return response
                                        .status(200)
                                        .header(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .sendString(
                                                Mono.just(body)
                                        )
                                        .then();
                            }

                            if (
                                    request.uri()
                                            .startsWith(
                                                    "/internal/events/details"
                                            )
                            ) {

                                String body =
                                        """
                                        [
                                          {
                                            "id": "%s",
                                            "name": "Concert",
                                            "venue": "Arena",
                                            "startsAt": "2026-09-15T20:00:00Z"
                                          }
                                        ]
                                        """.formatted(
                                                eventId
                                        );

                                return response
                                        .status(200)
                                        .header(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .sendString(
                                                Mono.just(body)
                                        )
                                        .then();
                            }

                            return response
                                    .status(404)
                                    .send();
                        })
                        .bindNow();

        String baseUrl =
                "http://localhost:"
                        + server.port();

        ReservationProxyController controller =
                new ReservationProxyController(
                        proxyForwarder,
                        baseUrl,
                        baseUrl,
                        WebClient.builder()
                                .build()
                );

        StepVerifier
                .create(
                        controller.findMine()
                )
                .expectNextCount(2)
                .verifyComplete();

        String eventRequestUri =
                requestedUris.stream()
                        .filter(uri ->
                                uri.startsWith(
                                        "/internal/events/details"
                                )
                        )
                        .findFirst()
                        .orElseThrow();

        assertThat(
                occurrences(
                        eventRequestUri,
                        eventId.toString()
                )
        ).isEqualTo(1);
    }

    private int occurrences(
            String value,
            String searchedValue
    ) {

        return (
                value.length()
                        - value.replace(
                        searchedValue,
                        ""
                ).length()
        ) / searchedValue.length();
    }
}