package fr.carrefour.bff.infrastructure.adapter.io.web;

import fr.carrefour.bff.infrastructure.configuration.security.AccessTokenRoleExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UserControllerTest {

    private static final String CLIENT_ID =
            "carrefour-kata-id";

    private static final String ACCESS_TOKEN =
            "access-token-value";

    private AccessTokenRoleExtractor roleExtractor;

    private OidcUser oidcUser;

    private OAuth2AuthorizedClient authorizedClient;

    private OAuth2AccessToken accessToken;

    private ServerWebExchange exchange;

    private UserController controller;

    @BeforeEach
    void setUp() {

        roleExtractor =
                mock(AccessTokenRoleExtractor.class);

        oidcUser =
                mock(OidcUser.class);

        authorizedClient =
                mock(OAuth2AuthorizedClient.class);

        accessToken =
                mock(OAuth2AccessToken.class);

        exchange =
                mock(ServerWebExchange.class);

        CsrfToken csrfToken = mock(CsrfToken.class);

        controller =
                new UserController(
                        roleExtractor,
                        CLIENT_ID
                );

        when(
                oidcUser.getPreferredUsername()
        ).thenReturn(
                "john.doe"
        );

        when(
                oidcUser.getEmail()
        ).thenReturn(
                "john.doe@example.com"
        );

        when(
                oidcUser.getFullName()
        ).thenReturn(
                "John Doe"
        );

        when(
                authorizedClient.getAccessToken()
        ).thenReturn(
                accessToken
        );

        when(
                accessToken.getTokenValue()
        ).thenReturn(
                ACCESS_TOKEN
        );

        doReturn(
                Mono.just(csrfToken)
        ).when(
                exchange
        ).getAttribute(
                CsrfToken.class.getName()
        );
    }

    @Test
    void shouldReturnCurrentAdministratorUser() {

        when(
                roleExtractor.hasClientRole(
                        ACCESS_TOKEN,
                        CLIENT_ID,
                        "administrator"
                )
        ).thenReturn(
                Mono.just(true)
        );

        Mono<CurrentUserResponse> result =
                controller.me(
                        oidcUser,
                        authorizedClient,
                        exchange
                );

        StepVerifier
                .create(result)
                .assertNext(response ->

                    assertThat(response)
                            .usingRecursiveComparison()
                            .isEqualTo(
                                    new CurrentUserResponse(
                                            "john.doe",
                                            "john.doe@example.com",
                                            "John Doe",
                                            true
                                    )
                            ))
                .verifyComplete();

        verify(
                roleExtractor
        ).hasClientRole(
                ACCESS_TOKEN,
                CLIENT_ID,
                "administrator"
        );
    }

    @Test
    void shouldReturnCurrentNonAdministratorUser() {

        when(
                roleExtractor.hasClientRole(
                        ACCESS_TOKEN,
                        CLIENT_ID,
                        "administrator"
                )
        ).thenReturn(
                Mono.just(false)
        );

        Mono<CurrentUserResponse> result =
                controller.me(
                        oidcUser,
                        authorizedClient,
                        exchange
                );

        StepVerifier
                .create(result)
                .assertNext(response ->

                    assertThat(response)
                            .usingRecursiveComparison()
                            .isEqualTo(
                                    new CurrentUserResponse(
                                            "john.doe",
                                            "john.doe@example.com",
                                            "John Doe",
                                            false
                                    )
                            ))
                .verifyComplete();
    }

    @Test
    void shouldUseAccessTokenToExtractAdministratorRole() {

        when(
                roleExtractor.hasClientRole(
                        ACCESS_TOKEN,
                        CLIENT_ID,
                        "administrator"
                )
        ).thenReturn(
                Mono.just(false)
        );

        controller
                .me(
                        oidcUser,
                        authorizedClient,
                        exchange
                )
                .block();

        verify(
                authorizedClient
        ).getAccessToken();

        verify(
                accessToken
        ).getTokenValue();

        verify(
                roleExtractor
        ).hasClientRole(
                ACCESS_TOKEN,
                CLIENT_ID,
                "administrator"
        );
    }

    @Test
    void shouldFailWhenCsrfTokenIsNotAvailable() {

        when(
                exchange.getAttribute(
                        CsrfToken.class.getName()
                )
        ).thenReturn(
                null
        );

        Mono<CurrentUserResponse> result =
                controller.me(
                        oidcUser,
                        authorizedClient,
                        exchange
                );

        StepVerifier
                .create(result)
                .expectErrorSatisfies(error ->

                    assertThat(error)
                            .isInstanceOf(
                                    IllegalStateException.class
                            )
                            .hasMessage(
                                    "CSRF token is not available"
                            ))
                .verify();

        verifyNoInteractions(
                roleExtractor
        );
    }

    @Test
    void shouldPropagateRoleExtractionFailure() {

        RuntimeException failure =
                new RuntimeException(
                        "Invalid access token"
                );

        when(
                roleExtractor.hasClientRole(
                        ACCESS_TOKEN,
                        CLIENT_ID,
                        "administrator"
                )
        ).thenReturn(
                Mono.error(failure)
        );

        Mono<CurrentUserResponse> result =
                controller.me(
                        oidcUser,
                        authorizedClient,
                        exchange
                );

        StepVerifier
                .create(result)
                .expectErrorSatisfies(error ->
                        assertThat(error)
                                .isSameAs(failure)
                )
                .verify();
    }
}