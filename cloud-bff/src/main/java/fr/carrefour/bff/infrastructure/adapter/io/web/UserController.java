package fr.carrefour.bff.infrastructure.adapter.io.web;

import fr.carrefour.bff.infrastructure.configuration.security.AccessTokenRoleExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.zip;

@RestController
public class UserController {

    private final AccessTokenRoleExtractor roleExtractor;
    private final String clientId;

    public UserController(
            AccessTokenRoleExtractor roleExtractor,
            @Value("${spring.security.oauth2.client.registration.cloud-bff.client-id}")
            String clientId
    ) {
        this.roleExtractor = roleExtractor;
        this.clientId = clientId;
    }

    @GetMapping("/api/me")
    public Mono<CurrentUserResponse> me(
            @AuthenticationPrincipal OidcUser user,
            @RegisteredOAuth2AuthorizedClient("cloud-bff")
            OAuth2AuthorizedClient authorizedClient,
            ServerWebExchange exchange
    ) {

        String accessToken =
                authorizedClient
                        .getAccessToken()
                        .getTokenValue();

        Mono<CsrfToken> csrfToken =
                exchange.getAttribute(
                        CsrfToken.class.getName()
                );

        if (csrfToken == null) {
            return error(
                    new IllegalStateException(
                            "CSRF token is not available"
                    )
            );
        }

        return zip(
                roleExtractor.hasClientRole(
                        accessToken,
                        clientId,
                        "administrator"
                ),
                csrfToken
        ).map(tuple ->
                new CurrentUserResponse(
                        user.getPreferredUsername(),
                        user.getEmail(),
                        user.getFullName(),
                        tuple.getT1()
                )
        );
    }
}
