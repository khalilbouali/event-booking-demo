package fr.carrefour.bff.infrastructure.configuration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static fr.carrefour.bff.infrastructure.filter.CorrelationIdFilter.CONTEXT_KEY;
import static fr.carrefour.bff.infrastructure.filter.CorrelationIdFilter.HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE;

class WebClientConfigurationTest {

    private static final String ACCESS_TOKEN =
            "downstream-access-token";

    private static final String CORRELATION_ID =
            "correlation-123";

    private ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

    private WebClient webClient;

    private DisposableServer server;

    private AtomicReference<String> authorizationHeader;

    private AtomicReference<String> correlationHeader;

    @BeforeEach
    void setUp() {

        authorizedClientManager =
                mock(
                        ReactiveOAuth2AuthorizedClientManager.class
                );

        authorizationHeader =
                new AtomicReference<>();

        correlationHeader =
                new AtomicReference<>();

        server =
                HttpServer.create()
                        .port(0)
                        .handle((request, response) -> {

                            authorizationHeader.set(
                                    request
                                            .requestHeaders()
                                            .get(
                                                    HttpHeaders.AUTHORIZATION
                                            )
                            );

                            correlationHeader.set(
                                    request
                                            .requestHeaders()
                                            .get(
                                                    HEADER_NAME
                                            )
                            );

                            return response
                                    .status(200)
                                    .sendString(
                                            Mono.just("OK")
                                    )
                                    .then();
                        })
                        .bindNow();

        WebClientConfiguration configuration =
                new WebClientConfiguration();

        webClient =
                configuration.microserviceWebClient(
                        authorizedClientManager
                );
    }

    @AfterEach
    void tearDown() {

        if (server != null) {
            server.disposeNow();
        }
    }

    @Test
    void shouldRelayOAuth2AccessTokenToDownstreamService() {

        ClientRegistration registration =
                clientRegistration();

        OAuth2AuthenticationToken authentication =
                authentication();

        OAuth2AccessToken accessToken =
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        ACCESS_TOKEN,
                        Instant.now(),
                        Instant.now()
                                .plusSeconds(300)
                );

        OAuth2AuthorizedClient authorizedClient =
                new OAuth2AuthorizedClient(
                        registration,
                        authentication.getName(),
                        accessToken
                );

        when(
                authorizedClientManager.authorize(
                        any(OAuth2AuthorizeRequest.class)
                )
        ).thenReturn(
                Mono.just(authorizedClient)
        );

        Mono<String> result =
                webClient
                        .get()
                        .uri(
                                "http://localhost:"
                                        + server.port()
                                        + "/test"
                        )
                        .retrieve()
                        .bodyToMono(String.class)
                        .contextWrite(
                                org.springframework.security.core.context
                                        .ReactiveSecurityContextHolder
                                        .withAuthentication(
                                                authentication
                                        )
                        );

        StepVerifier
                .create(result)
                .expectNext("OK")
                .verifyComplete();

        assertThat(
                authorizationHeader.get()
        ).isEqualTo(
                "Bearer " + ACCESS_TOKEN
        );

        verify(
                authorizedClientManager,
                atLeastOnce()
        ).authorize(
                any(OAuth2AuthorizeRequest.class)
        );
    }

    @Test
    void shouldPropagateCorrelationIdFromReactorContext() {

        Mono<String> result =
                webClient
                        .get()
                        .uri(
                                "http://localhost:"
                                        + server.port()
                                        + "/test"
                        )
                        .retrieve()
                        .bodyToMono(String.class)
                        .contextWrite(context ->
                                context.put(
                                        CONTEXT_KEY,
                                        CORRELATION_ID
                                )
                        );

        StepVerifier
                .create(result)
                .expectNext("OK")
                .verifyComplete();

        assertThat(
                correlationHeader.get()
        ).isEqualTo(
                CORRELATION_ID
        );
    }

    @Test
    void shouldNotAddCorrelationHeaderWhenContextDoesNotContainCorrelationId() {

        Mono<String> result =
                webClient
                        .get()
                        .uri(
                                "http://localhost:"
                                        + server.port()
                                        + "/test"
                        )
                        .retrieve()
                        .bodyToMono(String.class);

        StepVerifier
                .create(result)
                .expectNext("OK")
                .verifyComplete();

        assertThat(
                correlationHeader.get()
        ).isNull();
    }

    private ClientRegistration clientRegistration() {

        return ClientRegistration
                .withRegistrationId("cloud-bff")
                .clientId("test-client")
                .clientSecret("test-secret")
                .authorizationGrantType(
                        AUTHORIZATION_CODE
                )
                .redirectUri(
                        "http://localhost/login/oauth2/code/cloud-bff"
                )
                .authorizationUri(
                        "https://keycloak.test/authorize"
                )
                .tokenUri(
                        "https://keycloak.test/token"
                )
                .build();
    }

    private OAuth2AuthenticationToken authentication() {

        DefaultOAuth2User principal =
                new DefaultOAuth2User(
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_USER"
                                )
                        ),
                        Map.of(
                                "sub",
                                "user-123"
                        ),
                        "sub"
                );

        return new OAuth2AuthenticationToken(
                principal,
                principal.getAuthorities(),
                "cloud-bff"
        );
    }
}