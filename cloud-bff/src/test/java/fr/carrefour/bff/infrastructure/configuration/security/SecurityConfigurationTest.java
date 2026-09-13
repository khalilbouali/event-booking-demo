package fr.carrefour.bff.infrastructure.configuration.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.*;

@WebFluxTest(
        controllers = SecurityConfigurationTest.TestController.class,
        properties = {
                "server.reactive.session.cookie.secure=false",

                "spring.security.oauth2.client.registration.cloud-bff.client-id=test-client",
                "spring.security.oauth2.client.registration.cloud-bff.client-secret=test-secret",
                "spring.security.oauth2.client.registration.cloud-bff.provider=keycloak",
                "spring.security.oauth2.client.registration.cloud-bff.authorization-grant-type=authorization_code",
                "spring.security.oauth2.client.registration.cloud-bff.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}",

                "spring.security.oauth2.client.registration.cloud-bff.scope[0]=openid",
                "spring.security.oauth2.client.registration.cloud-bff.scope[1]=profile",
                "spring.security.oauth2.client.registration.cloud-bff.scope[2]=email",

                "spring.security.oauth2.client.provider.keycloak.authorization-uri=https://keycloak.test/authorize",
                "spring.security.oauth2.client.provider.keycloak.token-uri=https://keycloak.test/token",
                "spring.security.oauth2.client.provider.keycloak.jwk-set-uri=https://keycloak.test/jwks",
                "spring.security.oauth2.client.provider.keycloak.user-info-uri=https://keycloak.test/userinfo",
                "spring.security.oauth2.client.provider.keycloak.user-name-attribute=sub"
        }
)
@Import({
        SecurityConfiguration.class,
        SecurityConfigurationTest.TestController.class
})
class SecurityConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private ServerLogoutSuccessHandler oidcLogoutSuccessHandler;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {

        when(
                oidcLogoutSuccessHandler.onLogoutSuccess(
                        any(),
                        any()
                )
        ).thenReturn(
                Mono.empty()
        );

        webTestClient =
                WebTestClient
                        .bindToApplicationContext(
                                applicationContext
                        )
                        .apply(
                                springSecurity()
                        )
                        .configureClient()
                        .build();
    }

    @Test
    void shouldReturnUnauthorizedForApiWhenNotAuthenticated() {

        webTestClient
                .get()
                .uri("/api/protected")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void shouldAllowAuthenticatedUserToAccessApi() {

        webTestClient
                .mutateWith(
                        mockOidcLogin()
                )
                .get()
                .uri("/api/protected")
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void shouldAllowPublicEndpointWithoutAuthentication() {

        webTestClient
                .get()
                .uri("/public")
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void shouldAllowActuatorHealthWithoutAuthentication() {

        webTestClient
                .get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void shouldAllowActuatorInfoWithoutAuthentication() {

        webTestClient
                .get()
                .uri("/actuator/info")
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void shouldRejectAuthenticatedPostWithoutCsrfToken() {

        webTestClient
                .mutateWith(
                        mockOidcLogin()
                )
                .post()
                .uri("/api/protected")
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void shouldAllowAuthenticatedPostWithCsrfToken() {

        webTestClient
                .mutateWith(
                        mockOidcLogin()
                )
                .mutateWith(
                        csrf()
                )
                .post()
                .uri("/api/protected")
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void shouldUseConfiguredLogoutSuccessHandler() {

        webTestClient
                .mutateWith(
                        mockOidcLogin()
                )
                .mutateWith(
                        csrf()
                )
                .post()
                .uri("/logout")
                .exchange()
                .expectStatus()
                .is2xxSuccessful();

        verify(
                oidcLogoutSuccessHandler
        ).onLogoutSuccess(
                any(),
                any()
        );
    }

    @RestController
    static class TestController {

        @GetMapping("/api/protected")
        String protectedGet() {
            return "OK";
        }

        @PostMapping("/api/protected")
        String protectedPost() {
            return "OK";
        }

        @GetMapping("/public")
        String publicEndpoint() {
            return "PUBLIC";
        }

        @GetMapping("/actuator/health")
        String health() {
            return "UP";
        }

        @GetMapping("/actuator/info")
        String info() {
            return "INFO";
        }
    }
}
