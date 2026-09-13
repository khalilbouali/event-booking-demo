package fr.carrefour.bff.infrastructure.adapter.io.web;

import io.netty.handler.timeout.ReadTimeoutException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.ResponseEntity.status;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.just;

@Component
public class ProxyErrorHandler {

    public Mono<ResponseEntity<byte[]>> handle(
            Throwable throwable
    ) {

        if (throwable instanceof TimeoutException ||
                throwable instanceof ReadTimeoutException) {

            return just(
                    status(GATEWAY_TIMEOUT)
                            .body(new byte[0])
            );
        }

        if (throwable instanceof WebClientRequestException) {

            return just(
                    status(BAD_GATEWAY)
                            .body(new byte[0])
            );
        }

        return error(throwable);
    }
}
