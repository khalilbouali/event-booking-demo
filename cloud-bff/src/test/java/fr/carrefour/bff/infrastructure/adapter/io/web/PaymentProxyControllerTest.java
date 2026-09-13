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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentProxyControllerTest {

    private static final String PAYMENT_SERVICE_URL =
            "http://payment-ms:8080";

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

        PaymentProxyController controller =
                new PaymentProxyController(
                        proxyForwarder,
                        WebClient.builder().build(),
                        PAYMENT_SERVICE_URL,
                        RESERVATION_SERVICE_URL,
                        EVENT_SERVICE_URL
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
    void shouldForwardPaymentRequestToPaymentService() {

        byte[] responseBody =
                """
                {"id":"payment-123"}
                """.getBytes();

        when(
                proxyForwarder.forward(
                        any(ServerWebExchange.class),
                        eq(PAYMENT_SERVICE_URL)
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
                .uri("/api/payments")
                .bodyValue(
                        """
                        {"reservationId":"reservation-123"}
                        """
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
                eq(PAYMENT_SERVICE_URL)
        );
    }

    @Test
    void shouldPropagatePaymentServiceStatus() {

        when(
                proxyForwarder.forward(
                        any(ServerWebExchange.class),
                        eq(PAYMENT_SERVICE_URL)
                )
        ).thenReturn(
                Mono.just(
                        ResponseEntity
                                .status(409)
                                .build()
                )
        );

        webTestClient
                .post()
                .uri("/api/payments")
                .exchange()
                .expectStatus()
                .isEqualTo(409);

        verify(
                proxyForwarder
        ).forward(
                any(ServerWebExchange.class),
                eq(PAYMENT_SERVICE_URL)
        );
    }

    @Test
    void shouldReturnEmptyFluxWithoutLoadingReservationsWhenNoPaymentsExist() {

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

        PaymentProxyController controller =
                new PaymentProxyController(
                        proxyForwarder,
                        WebClient.builder().build(),
                        baseUrl,
                        baseUrl,
                        baseUrl
                );

        StepVerifier
                .create(
                        controller.findMine()
                )
                .verifyComplete();

        assertThat(
                requestedUris
        ).containsExactly(
                "/api/payments"
        );

        verifyNoInteractions(
                proxyForwarder
        );
    }

    @Test
    void shouldEnrichPaymentWithReservationAndEventDetails() {

        UUID paymentId =
                UUID.randomUUID();

        UUID reservationId =
                UUID.randomUUID();

        UUID eventId =
                UUID.randomUUID();

        Instant paymentCreatedAt =
                Instant.parse(
                        "2026-09-10T08:00:00Z"
                );

        Instant eventStartsAt =
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
                                                    "/api/payments"
                                            )
                            ) {

                                String body =
                                        """
                                        [
                                          {
                                            "id": "%s",
                                            "reservationId": "%s",
                                            "customerId": "customer-123",
                                            "amount": 250.00,
                                            "status": "SUCCEEDED",
                                            "createdAt": "%s"
                                          }
                                        ]
                                        """.formatted(
                                                paymentId,
                                                reservationId,
                                                paymentCreatedAt
                                        );

                                return json(
                                        response,
                                        body
                                );
                            }

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
                                            "createdAt": "2026-09-10T07:55:00Z",
                                            "expiresAt": "2026-09-10T08:10:00Z"
                                          }
                                        ]
                                        """.formatted(
                                                reservationId,
                                                eventId
                                        );

                                return json(
                                        response,
                                        body
                                );
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
                                                eventStartsAt
                                        );

                                return json(
                                        response,
                                        body
                                );
                            }

                            return response
                                    .status(404)
                                    .send();
                        })
                        .bindNow();

        String baseUrl =
                "http://localhost:"
                        + server.port();

        PaymentProxyController controller =
                controller(baseUrl);

        StepVerifier
                .create(
                        controller.findMine()
                )
                .assertNext(view -> {

                    assertThat(
                            view.id()
                    ).isEqualTo(
                            paymentId
                    );

                    assertThat(
                            view.reservationId()
                    ).isEqualTo(
                            reservationId
                    );

                    assertThat(
                            view.amount()
                    ).isEqualByComparingTo(
                            new BigDecimal("250.00")
                    );

                    assertThat(
                            view.status()
                    ).isEqualTo(
                            "SUCCEEDED"
                    );

                    assertThat(
                            view.createdAt()
                    ).isEqualTo(
                            paymentCreatedAt
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
                            eventStartsAt
                    );
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnUnknownEventWhenReservationDoesNotExist() {

        UUID paymentId =
                UUID.randomUUID();

        UUID reservationId =
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
                                                    "/api/payments"
                                            )
                            ) {

                                String body =
                                        """
                                        [
                                          {
                                            "id": "%s",
                                            "reservationId": "%s",
                                            "customerId": "customer-123",
                                            "amount": 150.00,
                                            "status": "SUCCEEDED",
                                            "createdAt": "2026-09-10T08:00:00Z"
                                          }
                                        ]
                                        """.formatted(
                                                paymentId,
                                                reservationId
                                        );

                                return json(
                                        response,
                                        body
                                );
                            }

                            if (
                                    request.uri()
                                            .equals(
                                                    "/api/reservations"
                                            )
                            ) {

                                return json(
                                        response,
                                        "[]"
                                );
                            }

                            return response
                                    .status(500)
                                    .send();
                        })
                        .bindNow();

        String baseUrl =
                "http://localhost:"
                        + server.port();

        PaymentProxyController controller =
                controller(baseUrl);

        StepVerifier
                .create(
                        controller.findMine()
                )
                .assertNext(view -> {

                    assertThat(
                            view.id()
                    ).isEqualTo(
                            paymentId
                    );

                    assertThat(
                            view.reservationId()
                    ).isEqualTo(
                            reservationId
                    );

                    assertThat(
                            view.eventId()
                    ).isNull();

                    assertThat(
                            view.seatId()
                    ).isNull();

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

        assertThat(
                requestedUris
        ).containsExactly(
                "/api/payments",
                "/api/reservations"
        );
    }

    @Test
    void shouldUseUnknownEventWhenEventDetailsAreMissing() {

        UUID paymentId =
                UUID.randomUUID();

        UUID reservationId =
                UUID.randomUUID();

        UUID eventId =
                UUID.randomUUID();

        server =
                HttpServer.create()
                        .port(0)
                        .handle((request, response) -> {

                            if (
                                    request.uri()
                                            .equals(
                                                    "/api/payments"
                                            )
                            ) {

                                String body =
                                        """
                                        [
                                          {
                                            "id": "%s",
                                            "reservationId": "%s",
                                            "customerId": "customer-123",
                                            "amount": 99.99,
                                            "status": "SUCCEEDED",
                                            "createdAt": "2026-09-10T08:00:00Z"
                                          }
                                        ]
                                        """.formatted(
                                                paymentId,
                                                reservationId
                                        );

                                return json(
                                        response,
                                        body
                                );
                            }

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
                                            "seatId": "B-7",
                                            "customerId": "customer-123",
                                            "status": "CONFIRMED",
                                            "createdAt": "2026-09-10T07:50:00Z",
                                            "expiresAt": "2026-09-10T08:05:00Z"
                                          }
                                        ]
                                        """.formatted(
                                                reservationId,
                                                eventId
                                        );

                                return json(
                                        response,
                                        body
                                );
                            }

                            if (
                                    request.uri()
                                            .startsWith(
                                                    "/internal/events/details"
                                            )
                            ) {

                                return json(
                                        response,
                                        "[]"
                                );
                            }

                            return response
                                    .status(404)
                                    .send();
                        })
                        .bindNow();

        String baseUrl =
                "http://localhost:"
                        + server.port();

        PaymentProxyController controller =
                controller(baseUrl);

        StepVerifier
                .create(
                        controller.findMine()
                )
                .assertNext(view -> {

                    assertThat(
                            view.eventId()
                    ).isEqualTo(
                            eventId
                    );

                    assertThat(
                            view.seatId()
                    ).isEqualTo(
                            "B-7"
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
    void shouldHandleMissingReservationWhenOtherPaymentHasReservation() {

        UUID payment1Id =
                UUID.randomUUID();

        UUID payment2Id =
                UUID.randomUUID();

        UUID reservation1Id =
                UUID.randomUUID();

        UUID missingReservationId =
                UUID.randomUUID();

        UUID eventId =
                UUID.randomUUID();

        server =
                HttpServer.create()
                        .port(0)
                        .handle((request, response) -> {

                            if (
                                    request.uri()
                                            .equals(
                                                    "/api/payments"
                                            )
                            ) {

                                String body =
                                        """
                                        [
                                          {
                                            "id": "%s",
                                            "reservationId": "%s",
                                            "customerId": "customer-123",
                                            "amount": 200.00,
                                            "status": "SUCCEEDED",
                                            "createdAt": "2026-09-10T08:00:00Z"
                                          },
                                          {
                                            "id": "%s",
                                            "reservationId": "%s",
                                            "customerId": "customer-123",
                                            "amount": 300.00,
                                            "status": "SUCCEEDED",
                                            "createdAt": "2026-09-10T08:01:00Z"
                                          }
                                        ]
                                        """.formatted(
                                                payment1Id,
                                                reservation1Id,
                                                payment2Id,
                                                missingReservationId
                                        );

                                return json(
                                        response,
                                        body
                                );
                            }

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
                                            "seatId": "C-1",
                                            "customerId": "customer-123",
                                            "status": "CONFIRMED",
                                            "createdAt": "2026-09-10T07:50:00Z",
                                            "expiresAt": "2026-09-10T08:05:00Z"
                                          }
                                        ]
                                        """.formatted(
                                                reservation1Id,
                                                eventId
                                        );

                                return json(
                                        response,
                                        body
                                );
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

                                return json(
                                        response,
                                        body
                                );
                            }

                            return response
                                    .status(404)
                                    .send();
                        })
                        .bindNow();

        String baseUrl =
                "http://localhost:"
                        + server.port();

        PaymentProxyController controller =
                controller(baseUrl);

        StepVerifier
                .create(
                        controller.findMine()
                )
                .assertNext(view -> {

                    assertThat(
                            view.id()
                    ).isEqualTo(
                            payment1Id
                    );

                    assertThat(
                            view.eventId()
                    ).isEqualTo(
                            eventId
                    );

                    assertThat(
                            view.seatId()
                    ).isEqualTo(
                            "C-1"
                    );

                    assertThat(
                            view.eventName()
                    ).isEqualTo(
                            "Concert"
                    );
                })
                .assertNext(view -> {

                    assertThat(
                            view.id()
                    ).isEqualTo(
                            payment2Id
                    );

                    assertThat(
                            view.reservationId()
                    ).isEqualTo(
                            missingReservationId
                    );

                    assertThat(
                            view.eventId()
                    ).isNull();

                    assertThat(
                            view.seatId()
                    ).isNull();

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

        UUID payment1Id =
                UUID.randomUUID();

        UUID payment2Id =
                UUID.randomUUID();

        UUID reservation1Id =
                UUID.randomUUID();

        UUID reservation2Id =
                UUID.randomUUID();

        UUID eventId =
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
                                                    "/api/payments"
                                            )
                            ) {

                                String body =
                                        """
                                        [
                                          {
                                            "id": "%s",
                                            "reservationId": "%s",
                                            "customerId": "customer-123",
                                            "amount": 100.00,
                                            "status": "SUCCEEDED",
                                            "createdAt": "2026-09-10T08:00:00Z"
                                          },
                                          {
                                            "id": "%s",
                                            "reservationId": "%s",
                                            "customerId": "customer-123",
                                            "amount": 150.00,
                                            "status": "SUCCEEDED",
                                            "createdAt": "2026-09-10T08:01:00Z"
                                          }
                                        ]
                                        """.formatted(
                                                payment1Id,
                                                reservation1Id,
                                                payment2Id,
                                                reservation2Id
                                        );

                                return json(
                                        response,
                                        body
                                );
                            }

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
                                            "createdAt": "2026-09-10T07:50:00Z",
                                            "expiresAt": "2026-09-10T08:05:00Z"
                                          },
                                          {
                                            "id": "%s",
                                            "eventId": "%s",
                                            "seatId": "A-2",
                                            "customerId": "customer-123",
                                            "status": "CONFIRMED",
                                            "createdAt": "2026-09-10T07:51:00Z",
                                            "expiresAt": "2026-09-10T08:06:00Z"
                                          }
                                        ]
                                        """.formatted(
                                                reservation1Id,
                                                eventId,
                                                reservation2Id,
                                                eventId
                                        );

                                return json(
                                        response,
                                        body
                                );
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

                                return json(
                                        response,
                                        body
                                );
                            }

                            return response
                                    .status(404)
                                    .send();
                        })
                        .bindNow();

        String baseUrl =
                "http://localhost:"
                        + server.port();

        PaymentProxyController controller =
                controller(baseUrl);

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

    private PaymentProxyController controller(
            String baseUrl
    ) {

        return new PaymentProxyController(
                proxyForwarder,
                WebClient.builder().build(),
                baseUrl,
                baseUrl,
                baseUrl
        );
    }

    private reactor.core.publisher.Mono<Void> json(
            reactor.netty.http.server.HttpServerResponse response,
            String body
    ) {

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