package fr.carrefour.bff.infrastructure.adapter.io.web;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Set;

import static java.net.URI.create;
import static java.util.Set.of;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.ResponseEntity.status;
import static org.springframework.web.reactive.function.BodyInserters.fromDataBuffers;

@Component
public class ProxyForwarder {

    private static final Set<String> REQUEST_HEADERS_TO_SKIP = of(
            HOST.toLowerCase(),
            AUTHORIZATION.toLowerCase(),
            COOKIE.toLowerCase(),
            CONTENT_LENGTH.toLowerCase(),
            CONNECTION.toLowerCase(),
            "x-xsrf-token"
    );

    private static final Set<String> RESPONSE_HEADERS_TO_SKIP = of(
            TRANSFER_ENCODING.toLowerCase(),
            CONTENT_LENGTH.toLowerCase(),
            CONNECTION.toLowerCase()
    );

    private final WebClient webClient;
    private final ProxyErrorHandler proxyErrorHandler;

    public ProxyForwarder(
            @Qualifier("microserviceWebClient") WebClient webClient,
            ProxyErrorHandler proxyErrorHandler
    ) {
        this.webClient = webClient;
        this.proxyErrorHandler = proxyErrorHandler;
    }

    public Mono<ResponseEntity<byte[]>> forward(
            ServerWebExchange exchange,
            String targetBaseUrl
    ) {

        HttpMethod method = exchange.getRequest().getMethod();

        URI targetUri = buildTargetUri(
                targetBaseUrl,
                exchange.getRequest().getURI()
        );

        RequestBodySpec request = webClient
                .method(method)
                .uri(targetUri)
                .headers(headers ->
                        copyRequestHeaders(
                                exchange.getRequest().getHeaders(),
                                headers
                        )
                );

        return request
                .body(
                        fromDataBuffers(
                                exchange.getRequest().getBody()
                        )
                )
                .exchangeToMono(response ->
                        response.bodyToMono(byte[].class)
                                .defaultIfEmpty(new byte[0])
                                .map(body -> {

                                    BodyBuilder responseBuilder =
                                            status(
                                                    response.statusCode()
                                            );

                                    response.headers()
                                            .asHttpHeaders()
                                            .forEach((name, values) -> {

                                                if (!RESPONSE_HEADERS_TO_SKIP.contains(
                                                        name.toLowerCase()
                                                )) {
                                                    responseBuilder.header(
                                                            name,
                                                            values.toArray(String[]::new)
                                                    );
                                                }
                                            });

                                    return responseBuilder.body(body);
                                })
                )
                .onErrorResume(
                        proxyErrorHandler::handle
                );
    }

    private URI buildTargetUri(
            String targetBaseUrl,
            URI incomingUri
    ) {

        String query = incomingUri.getRawQuery();

        String uri = targetBaseUrl + incomingUri.getRawPath();

        if (query != null && !query.isBlank()) {
            uri += "?" + query;
        }

        return create(uri);
    }

    private void copyRequestHeaders(
            HttpHeaders source,
            HttpHeaders target
    ) {

        source.forEach((name, values) -> {

            if (!REQUEST_HEADERS_TO_SKIP.contains(
                    name.toLowerCase()
            )) {
                target.put(name, values);
            }
        });
    }
}