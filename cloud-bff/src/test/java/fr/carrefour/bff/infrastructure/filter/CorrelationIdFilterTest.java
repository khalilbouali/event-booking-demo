package fr.carrefour.bff.infrastructure.filter;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static fr.carrefour.bff.infrastructure.filter.CorrelationIdFilter.CONTEXT_KEY;
import static fr.carrefour.bff.infrastructure.filter.CorrelationIdFilter.HEADER_NAME;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
    }

    @Test
    void shouldPreserveExistingCorrelationId() {

        String correlationId =
                "correlation-123";

        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/events")
                                .header(
                                        HEADER_NAME,
                                        correlationId
                                )
                                .build()
                );

        AtomicReference<@Nullable ServerWebExchange> forwardedExchange =
                new AtomicReference<>();

        AtomicReference<@Nullable String> contextCorrelationId =
                new AtomicReference<>();

        WebFilterChain chain =
                filteredExchange ->
                        Mono.deferContextual(context -> {

                            forwardedExchange.set(
                                    filteredExchange
                            );

                            contextCorrelationId.set(
                                    context.get(CONTEXT_KEY)
                            );

                            return Mono.empty();
                        });

        StepVerifier
                .create(
                        filter.filter(
                                exchange,
                                chain
                        )
                )
                .verifyComplete();

        assertThat(
                requireNonNull(forwardedExchange
                                .get())
                        .getRequest()
                        .getHeaders()
                        .getFirst(HEADER_NAME)
        ).isEqualTo(correlationId);

        assertThat(
                exchange
                        .getResponse()
                        .getHeaders()
                        .getFirst(HEADER_NAME)
        ).isEqualTo(correlationId);

        assertThat(
                contextCorrelationId.get()
        ).isEqualTo(correlationId);
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing() {

        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/events")
                                .build()
                );

        AtomicReference<@Nullable ServerWebExchange> forwardedExchange =
                new AtomicReference<>();

        AtomicReference<@Nullable String> contextCorrelationId =
                new AtomicReference<>();

        WebFilterChain chain =
                filteredExchange ->
                        Mono.deferContextual(context -> {

                            forwardedExchange.set(
                                    filteredExchange
                            );

                            contextCorrelationId.set(
                                    context.get(CONTEXT_KEY)
                            );

                            return Mono.empty();
                        });

        StepVerifier
                .create(
                        filter.filter(
                                exchange,
                                chain
                        )
                )
                .verifyComplete();

        String generatedCorrelationId =
                requireNonNull(forwardedExchange
                        .get())
                        .getRequest()
                        .getHeaders()
                        .getFirst(HEADER_NAME);

        assertThat(
                generatedCorrelationId
        ).isNotBlank();

        // Your implementation generates a UUID.
        assertThat(
                UUID.fromString(
                        generatedCorrelationId
                )
        ).isNotNull();

        assertThat(
                exchange
                        .getResponse()
                        .getHeaders()
                        .getFirst(HEADER_NAME)
        ).isEqualTo(
                generatedCorrelationId
        );

        assertThat(
                contextCorrelationId.get()
        ).isEqualTo(
                generatedCorrelationId
        );
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsBlank() {

        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/events")
                                .header(
                                        HEADER_NAME,
                                        "   "
                                )
                                .build()
                );

        AtomicReference<@Nullable ServerWebExchange> forwardedExchange =
                new AtomicReference<>();

        AtomicReference<@Nullable String> contextCorrelationId =
                new AtomicReference<>();

        WebFilterChain chain =
                filteredExchange ->
                        Mono.deferContextual(context -> {

                            forwardedExchange.set(
                                    filteredExchange
                            );

                            contextCorrelationId.set(
                                    context.get(CONTEXT_KEY)
                            );

                            return Mono.empty();
                        });

        StepVerifier
                .create(
                        filter.filter(
                                exchange,
                                chain
                        )
                )
                .verifyComplete();

        String generatedCorrelationId =
                requireNonNull(forwardedExchange
                        .get())
                        .getRequest()
                        .getHeaders()
                        .getFirst(HEADER_NAME);

        assertThat(
                generatedCorrelationId
        ).isNotBlank();

        assertThat(
                generatedCorrelationId
        ).isNotEqualTo("   ");

        assertThat(
                UUID.fromString(
                        generatedCorrelationId
                )
        ).isNotNull();

        assertThat(
                exchange
                        .getResponse()
                        .getHeaders()
                        .getFirst(HEADER_NAME)
        ).isEqualTo(
                generatedCorrelationId
        );

        assertThat(
                contextCorrelationId.get()
        ).isEqualTo(
                generatedCorrelationId
        );
    }

    @Test
    void shouldPropagateDownstreamFailure() {

        RuntimeException failure =
                new RuntimeException(
                        "Downstream filter failed"
                );

        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/events")
                                .header(
                                        HEADER_NAME,
                                        "correlation-123"
                                )
                                .build()
                );

        WebFilterChain chain =
                filteredExchange ->
                        Mono.error(failure);

        StepVerifier
                .create(
                        filter.filter(
                                exchange,
                                chain
                        )
                )
                .expectErrorSatisfies(error ->
                        assertThat(error)
                                .isSameAs(failure)
                )
                .verify();
    }
}
