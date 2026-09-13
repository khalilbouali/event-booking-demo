package fr.carrefour.reservation.infrastructure.configuration.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@WebFluxTest(
        controllers = SecurityConfigurationTest.TestController.class,
        properties = {
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/mock-jwks",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/mock-issuer"
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
    private ReactiveJwtDecoder reactiveJwtDecoder;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient =
                WebTestClient
                        .bindToApplicationContext(applicationContext)
                        .apply(springSecurity())
                        .configureClient()
                        .build();
    }

    @Test
    void shouldAllowHealthEndpointWithoutAuthentication() {

        webTestClient
                .get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void shouldAllowInfoEndpointWithoutAuthentication() {

        webTestClient
                .get()
                .uri("/actuator/info")
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void shouldRejectProtectedEndpointWithoutAuthentication() {

        webTestClient
                .get()
                .uri("/protected")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void shouldAllowProtectedEndpointWhenAuthenticated() {

        webTestClient
                .mutateWith(
                        mockJwt()
                )
                .get()
                .uri("/protected")
                .exchange()
                .expectStatus()
                .isOk();
    }

    @RestController
    static class TestController {

        @GetMapping("/actuator/health")
        String health() {
            return "UP";
        }

        @GetMapping("/actuator/info")
        String info() {
            return "INFO";
        }

        @GetMapping("/protected")
        String protectedEndpoint() {
            return "OK";
        }
    }
}