package fr.carrefour.bff.infrastructure.adapter.io.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class EventProxyController {

    private final ProxyForwarder proxyForwarder;
    private final String eventServiceUrl;

    public EventProxyController(
            ProxyForwarder proxyForwarder,
            @Value("${services.event.base-url}") String eventServiceUrl
    ) {
        this.proxyForwarder = proxyForwarder;
        this.eventServiceUrl = eventServiceUrl;
    }

    @RequestMapping("/api/events/**")
    Mono<ResponseEntity<byte[]>> proxy(
            ServerWebExchange exchange
    ) {
        return proxyForwarder.forward(
                exchange,
                eventServiceUrl
        );
    }
}
