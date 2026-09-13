package fr.carrefour.bff.infrastructure.adapter.io.web;

import io.netty.handler.timeout.ReadTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;

class ProxyErrorHandlerTest {

    private ProxyErrorHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProxyErrorHandler();
    }

    @Test
    void shouldReturnGatewayTimeoutForTimeoutException() {

        TimeoutException exception =
                new TimeoutException(
                        "Request timed out"
                );

        StepVerifier
                .create(
                        handler.handle(exception)
                )
                .assertNext(this::assertGatewayTimeout
                )
                .verifyComplete();
    }

    @Test
    void shouldReturnGatewayTimeoutForReadTimeoutException() {

        StepVerifier
                .create(
                        handler.handle(
                                ReadTimeoutException.INSTANCE
                        )
                )
                .assertNext(this::assertGatewayTimeout
                )
                .verifyComplete();
    }

    @Test
    void shouldReturnBadGatewayForWebClientRequestException() {

        WebClientRequestException exception =
                new WebClientRequestException(
                        new IOException(
                                "Connection refused"
                        ),
                        HttpMethod.GET,
                        URI.create(
                                "http://event-ms:8080/api/events"
                        ),
                        new HttpHeaders()
                );

        StepVerifier
                .create(
                        handler.handle(exception)
                )
                .assertNext(response -> {

                    assertThat(
                            response.getStatusCode()
                    ).isEqualTo(
                            BAD_GATEWAY
                    );

                    assertThat(
                            response.getBody()
                    ).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void shouldPropagateUnknownException() {

        IllegalStateException exception =
                new IllegalStateException(
                        "Unexpected failure"
                );

        StepVerifier
                .create(
                        handler.handle(exception)
                )
                .expectErrorSatisfies(error ->
                        assertThat(error)
                                .isSameAs(exception)
                )
                .verify();
    }

    private void assertGatewayTimeout(
            ResponseEntity<byte[]> response
    ) {

        assertThat(
                response.getStatusCode()
        ).isEqualTo(
                GATEWAY_TIMEOUT
        );

        assertThat(
                response.getBody()
        ).isEmpty();
    }
}