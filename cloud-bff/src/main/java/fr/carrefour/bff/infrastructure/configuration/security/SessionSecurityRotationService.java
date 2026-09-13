package fr.carrefour.bff.infrastructure.configuration.security;

import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public final class SessionSecurityRotationService {

    private final CookieServerCsrfTokenRepository csrfTokenRepository;

    public SessionSecurityRotationService(
            CookieServerCsrfTokenRepository csrfTokenRepository
    ) {
        this.csrfTokenRepository = csrfTokenRepository;
    }

    public Mono<Void> rotate(ServerWebExchange exchange) {

        return exchange.getSession()
                .flatMap(session ->
                        session.changeSessionId()
                                .then(
                                        csrfTokenRepository.generateToken(
                                                exchange
                                        )
                                )
                                .flatMap(csrfToken ->
                                        csrfTokenRepository.saveToken(
                                                exchange,
                                                csrfToken
                                        )
                                )
                );
    }
}