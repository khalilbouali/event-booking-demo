package fr.carrefour.bff.infrastructure.adapter.io.web;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ProxyForwarderTest {

    private DisposableServer server;

    @AfterEach
    void tearDown() {

        if (server != null) {
            server.disposeNow();
        }
    }

    @Test
    void shouldForwardMethodPathQueryBodyAndAllowedHeaders() {

        AtomicReference<@Nullable CapturedRequest> capturedRequest =
                new AtomicReference<>();

        server =
                HttpServer.create()
                        .port(0)
                        .handle((request, response) ->
                                request
                                        .receive()
                                        .aggregate()
                                        .asString()
                                        .defaultIfEmpty("")
                                        .flatMap(body -> {

                                            capturedRequest.set(
                                                    new CapturedRequest(
                                                            request.method().name(),
                                                            request.uri(),
                                                            request.requestHeaders()
                                                                    .get("X-Custom-Header"),
                                                            request.requestHeaders()
                                                                    .get(HttpHeaders.AUTHORIZATION),
                                                            request.requestHeaders()
                                                                    .get(HttpHeaders.COOKIE),
                                                            request.requestHeaders()
                                                                    .get("X-XSRF-TOKEN"),
                                                            body
                                                    )
                                            );

                                            return response
                                                    .status(201)
                                                    .header(
                                                            "X-Downstream",
                                                            "event-service"
                                                    )
                                                    .sendString(
                                                            Mono.just(
                                                                    "created"
                                                            )
                                                    )
                                                    .then();
                                        })
                        )
                        .bindNow();

        ProxyErrorHandler proxyErrorHandler =
                mock(ProxyErrorHandler.class);

        WebClient webClient =
                WebClient.builder()
                        .build();

        ProxyForwarder forwarder =
                new ProxyForwarder(
                        webClient,
                        proxyErrorHandler
                );

        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .post(
                                        "/api/events/event-123"
                                                + "?includeSeats=true&page=2"
                                )
                                .header(
                                        "X-Custom-Header",
                                        "custom-value"
                                )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer browser-token"
                                )
                                .header(
                                        HttpHeaders.COOKIE,
                                        "SESSION=session-123"
                                )
                                .header(
                                        "X-XSRF-TOKEN",
                                        "csrf-token"
                                )
                                .body(
                                        """
                                        {"name":"Concert"}
                                        """
                                )
                );

        String targetBaseUrl =
                "http://localhost:" + server.port();

        StepVerifier
                .create(
                        forwarder.forward(
                                exchange,
                                targetBaseUrl
                        )
                )
                .assertNext(response -> {

                    assertThat(
                            response.getStatusCode()
                    ).isEqualTo(
                            HttpStatus.CREATED
                    );

                    assertThat(
                            response.getBody()
                    ).isEqualTo(
                            "created".getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

                    assertThat(
                            response.getHeaders()
                                    .getFirst("X-Downstream")
                    ).isEqualTo(
                            "event-service"
                    );
                })
                .verifyComplete();

        CapturedRequest captured =
                requireNonNull(
                        capturedRequest.get()
                );

        assertThat(
                captured.method()
        ).isEqualTo(
                "POST"
        );

        assertThat(
                captured.uri()
        ).isEqualTo(
                "/api/events/event-123"
                        + "?includeSeats=true&page=2"
        );

        assertThat(
                captured.customHeader()
        ).isEqualTo(
                "custom-value"
        );

        assertThat(
                captured.authorization()
        ).isNull();

        assertThat(
                captured.cookie()
        ).isNull();

        assertThat(
                captured.xsrfToken()
        ).isNull();

        assertThat(
                captured.body()
        ).isEqualTo(
                """
                {"name":"Concert"}
                """
        );

        verifyNoInteractions(
                proxyErrorHandler
        );
    }

    @Test
    void shouldPropagateDownstreamStatusBodyAndAllowedResponseHeaders() {

        server =
                HttpServer.create()
                        .port(0)
                        .handle((request, response) ->
                                response
                                        .status(404)
                                        .header(
                                                "X-Downstream",
                                                "event-service"
                                        )
                                        .header(
                                                HttpHeaders.CONNECTION,
                                                "close"
                                        )
                                        .header(
                                                HttpHeaders.CONTENT_LENGTH,
                                                "15"
                                        )
                                        .sendString(
                                                Mono.just(
                                                        "Event not found"
                                                )
                                        )
                                        .then()
                        )
                        .bindNow();

        ProxyErrorHandler proxyErrorHandler =
                mock(ProxyErrorHandler.class);

        ProxyForwarder forwarder =
                new ProxyForwarder(
                        WebClient.builder().build(),
                        proxyErrorHandler
                );

        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get(
                                        "/api/events/unknown"
                                )
                                .build()
                );

        StepVerifier
                .create(
                        forwarder.forward(
                                exchange,
                                "http://localhost:"
                                        + server.port()
                        )
                )
                .assertNext(response -> {

                    assertThat(
                            response.getStatusCode()
                    ).isEqualTo(
                            HttpStatus.NOT_FOUND
                    );

                    assertThat(
                            response.getBody()
                    ).isEqualTo(
                            "Event not found".getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

                    assertThat(
                            response.getHeaders()
                                    .getFirst("X-Downstream")
                    ).isEqualTo(
                            "event-service"
                    );

                    assertThat(
                            response.getHeaders()
                                    .getFirst(
                                            HttpHeaders.CONNECTION
                                    )
                    ).isNull();

                    assertThat(
                            response.getHeaders()
                                    .getFirst(
                                            HttpHeaders.CONTENT_LENGTH
                                    )
                    ).isNull();
                })
                .verifyComplete();

        verifyNoInteractions(
                proxyErrorHandler
        );
    }

    @Test
    void shouldReturnEmptyByteArrayWhenDownstreamResponseHasNoBody() {

        server =
                HttpServer.create()
                        .port(0)
                        .handle((request, response) ->
                                response
                                        .status(204)
                                        .send()
                        )
                        .bindNow();

        ProxyErrorHandler proxyErrorHandler =
                mock(ProxyErrorHandler.class);

        ProxyForwarder forwarder =
                new ProxyForwarder(
                        WebClient.builder().build(),
                        proxyErrorHandler
                );

        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/events")
                                .build()
                );

        StepVerifier
                .create(
                        forwarder.forward(
                                exchange,
                                "http://localhost:"
                                        + server.port()
                        )
                )
                .assertNext(response -> {

                    assertThat(
                            response.getStatusCode()
                    ).isEqualTo(
                            HttpStatus.NO_CONTENT
                    );

                    assertThat(
                            response.getBody()
                    ).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void shouldDelegateWebClientFailureToProxyErrorHandler() {

        RuntimeException failure =
                new RuntimeException(
                        "Downstream unavailable"
                );

        WebClient failingWebClient =
                WebClient.builder()
                        .exchangeFunction(request ->
                                Mono.error(failure)
                        )
                        .build();

        ProxyErrorHandler proxyErrorHandler =
                mock(ProxyErrorHandler.class);

        byte[] errorBody =
                "Service unavailable".getBytes(
                        StandardCharsets.UTF_8
                );

        when(
                proxyErrorHandler.handle(failure)
        ).thenReturn(
                Mono.just(
                        ResponseEntity
                                .status(
                                        HttpStatus.BAD_GATEWAY
                                )
                                .body(errorBody)
                )
        );

        ProxyForwarder forwarder =
                new ProxyForwarder(
                        failingWebClient,
                        proxyErrorHandler
                );

        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/events")
                                .build()
                );

        StepVerifier
                .create(
                        forwarder.forward(
                                exchange,
                                "http://event-ms:8080"
                        )
                )
                .assertNext(response -> {

                    assertThat(
                            response.getStatusCode()
                    ).isEqualTo(
                            HttpStatus.BAD_GATEWAY
                    );

                    assertThat(
                            response.getBody()
                    ).isEqualTo(
                            errorBody
                    );
                })
                .verifyComplete();

        verify(
                proxyErrorHandler
        ).handle(failure);
    }

    private record CapturedRequest(
            String method,
            String uri,
            @Nullable String customHeader,
            @Nullable String authorization,
            @Nullable String cookie,
            @Nullable String xsrfToken,
            String body
    ) {
    }
}