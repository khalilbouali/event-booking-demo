package fr.carrefour.bff.infrastructure.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.oidc.web.server.logout
        .OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration
        .ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.server.authentication.logout
        .ServerLogoutSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import static org.springframework.web.util.UriComponentsBuilder.fromUriString;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.just;

@Configuration
public class LogoutConfiguration {

    @Bean
    ServerLogoutSuccessHandler oidcLogoutSuccessHandler(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            @Value("${app.security.oidc.logout-uri}")
            String logoutUri,
            @Value("${app.security.oidc.post-logout-redirect-uri}")
            String postLogoutRedirectUri
    ) {

        OidcClientInitiatedServerLogoutSuccessHandler handler =
                new OidcClientInitiatedServerLogoutSuccessHandler(
                        clientRegistrationRepository
                );

        handler.setRedirectUriResolver(parameters -> {

            if (!(parameters.getAuthentication().getPrincipal()
                    instanceof OidcUser oidcUser)) {
                return empty();
            }

            String redirectUri =
                    fromUriString(logoutUri)
                            .queryParam(
                                    "id_token_hint",
                                    oidcUser.getIdToken().getTokenValue()
                            )
                            .queryParam(
                                    "post_logout_redirect_uri",
                                    postLogoutRedirectUri
                            )
                            .build()
                            .encode()
                            .toUriString();

            return just(redirectUri);
        });

        return handler;
    }
}
