package fr.carrefour.bff.infrastructure.configuration.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizationSuccessHandler;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@Component
public final class RefreshRotationAuthorizationSuccessHandler
        implements ReactiveOAuth2AuthorizationSuccessHandler {

    private final ServerOAuth2AuthorizedClientRepository authorizedClientRepository;
    private final SessionSecurityRotationService rotationService;

    public RefreshRotationAuthorizationSuccessHandler(
            ServerOAuth2AuthorizedClientRepository authorizedClientRepository,
            SessionSecurityRotationService rotationService
    ) {
        this.authorizedClientRepository =
                authorizedClientRepository;

        this.rotationService =
                rotationService;
    }

    @Override
    public Mono<Void> onAuthorizationSuccess(
            OAuth2AuthorizedClient authorizedClient,
            Authentication principal,
            Map<String, Object> attributes
    ) {

        Object value =
                attributes.get(
                        ServerWebExchange.class.getName()
                );

        if (!(value instanceof ServerWebExchange exchange)) {
            return Mono.error(
                    new IllegalStateException(
                            "ServerWebExchange not available"
                    )
            );
        }

        String registrationId =
                authorizedClient
                        .getClientRegistration()
                        .getRegistrationId();

        return authorizedClientRepository
                .<OAuth2AuthorizedClient>loadAuthorizedClient(
                        registrationId,
                        principal,
                        exchange
                )
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(previousClient -> {

                    boolean refreshed =
                            previousClient
                                    .filter(previous ->
                                            previous.getRefreshToken() != null
                                    )
                                    .filter(previous ->
                                            !previous
                                                    .getAccessToken()
                                                    .getTokenValue()
                                                    .equals(
                                                            authorizedClient
                                                                    .getAccessToken()
                                                                    .getTokenValue()
                                                    )
                                    )
                                    .isPresent();

                    Mono<Void> save =
                            authorizedClientRepository
                                    .saveAuthorizedClient(
                                            authorizedClient,
                                            principal,
                                            exchange
                                    );

                    if (!refreshed) {
                        return save;
                    }

                    return save.then(
                            rotationService.rotate(exchange)
                    );
                });
    }
}