package fr.carrefour.bff.infrastructure.adapter.io.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EventProxyControllerTest {

    private static final String EVENT_SERVICE_URL =
            "http://event-ms:8080";

    private EventProxyController controller;
    private ProxyForwarder proxyForwarder;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {

        proxyForwarder =
                mock(ProxyForwarder.class);

        controller =
                new EventProxyController(
                        proxyForwarder,
                        EVENT_SERVICE_URL
                );

        webTestClient =
                WebTestClient
                        .bindToController(controller)
                        .build();
    }

    @Test
    void shouldForwardEventRequestToEventService() {

        byte[] responseBody =
                """
                {"id":"event-123"}
                """.getBytes();

        when(
                proxyForwarder.forward(
                        any(ServerWebExchange.class),
                        eq(EVENT_SERVICE_URL)
                )
        ).thenReturn(
                Mono.just(
                        ResponseEntity.ok(
                                responseBody
                        )
                )
        );

        webTestClient
                .get()
                .uri("/api/events/event-123")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(byte[].class)
                .isEqualTo(responseBody);

        verify(
                proxyForwarder
        ).forward(
                any(ServerWebExchange.class),
                eq(EVENT_SERVICE_URL)
        );
    }

    @Test
    void shouldForwardOriginalRequestPathAndQueryParameters() {

        when(
                proxyForwarder.forward(
                        any(ServerWebExchange.class),
                        eq(EVENT_SERVICE_URL)
                )
        ).thenReturn(
                Mono.just(
                        ResponseEntity.ok().build()
                )
        );

        webTestClient
                .get()
                .uri(
                        "/api/events/event-123?includeSeats=true"
                )
                .exchange()
                .expectStatus()
                .isOk();

        ArgumentCaptor<ServerWebExchange> exchangeCaptor =
                ArgumentCaptor.forClass(
                        ServerWebExchange.class
                );

        verify(
                proxyForwarder
        ).forward(
                exchangeCaptor.capture(),
                eq(EVENT_SERVICE_URL)
        );

        ServerWebExchange capturedExchange =
                exchangeCaptor.getValue();

        assertThat(
                capturedExchange
                        .getRequest()
                        .getPath()
                        .value()
        ).isEqualTo(
                "/api/events/event-123"
        );

        assertThat(
                capturedExchange
                        .getRequest()
                        .getQueryParams()
                        .getFirst("includeSeats")
        ).isEqualTo(
                "true"
        );
    }

    @Test
    void shouldPropagateResponseStatusFromForwarder() {

        byte[] responseBody =
                "Event not found".getBytes();

        when(
                proxyForwarder.forward(
                        any(ServerWebExchange.class),
                        eq(EVENT_SERVICE_URL)
                )
        ).thenReturn(
                Mono.just(
                        ResponseEntity
                                .status(404)
                                .body(responseBody)
                )
        );

        webTestClient
                .get()
                .uri("/api/events/unknown")
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(byte[].class)
                .isEqualTo(responseBody);
    }

    @Test
    void shouldPropagateForwarderFailure() {

        RuntimeException failure =
                new RuntimeException(
                        "Event service unavailable"
                );

        when(
                proxyForwarder.forward(
                        any(ServerWebExchange.class),
                        eq(EVENT_SERVICE_URL)
                )
        ).thenReturn(
                Mono.error(failure)
        );

        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get(
                                        "/api/events/event-123"
                                )
                                .build()
                );

        StepVerifier
                .create(
                        controller.proxy(
                                exchange
                        )
                )
                .expectErrorSatisfies(error ->
                        assertThat(error)
                                .isSameAs(failure)
                )
                .verify();

        verify(
                proxyForwarder
        ).forward(
                any(ServerWebExchange.class),
                eq(EVENT_SERVICE_URL)
        );
    }
}