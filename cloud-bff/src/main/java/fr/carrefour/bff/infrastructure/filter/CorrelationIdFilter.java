package fr.carrefour.bff.infrastructure.filter;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static java.util.UUID.randomUUID;

@Component
public class CorrelationIdFilter implements WebFilter {

    public static final String HEADER_NAME =
            "X-Correlation-Id";

    public static final String CONTEXT_KEY =
            "correlationId";

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain
    ) {

        String correlationId =
                exchange.getRequest()
                        .getHeaders()
                        .getFirst(HEADER_NAME);

        if (correlationId == null ||
                correlationId.isBlank()) {

            correlationId =
                    randomUUID().toString();
        }

        String finalCorrelationId =
                correlationId;

        ServerWebExchange mutatedExchange =
                exchange.mutate()
                        .request(request ->
                                request.headers(headers ->
                                        headers.set(
                                                HEADER_NAME,
                                                finalCorrelationId
                                        )
                                )
                        )
                        .build();

        mutatedExchange
                .getResponse()
                .getHeaders()
                .set(
                        HEADER_NAME,
                        finalCorrelationId
                );

        return chain
                .filter(mutatedExchange)
                .contextWrite(context ->
                        context.put(
                                CONTEXT_KEY,
                                finalCorrelationId
                        )
                );
    }
}
