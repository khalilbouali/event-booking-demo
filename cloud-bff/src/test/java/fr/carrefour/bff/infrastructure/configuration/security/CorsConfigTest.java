package fr.carrefour.bff.infrastructure.configuration.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    private CorsConfig corsConfig;

    @BeforeEach
    void setUp() {
        corsConfig = new CorsConfig();
    }

    @Test
    void shouldConfigureAllowedOrigins() {

        List<String> allowedOrigins =
                List.of(
                        "https://localhost",
                        "https://frontend.example.com"
                );

        UrlBasedCorsConfigurationSource source =
                corsConfig.corsConfigurationSource(
                        allowedOrigins
                );

        CorsConfiguration configuration =
                requireNonNull(
                        source.getCorsConfiguration(
                                exchange("/api/events")
                        )
                );

        assertThat(
                configuration.getAllowedOrigins()
        ).containsExactly(
                "https://localhost",
                "https://frontend.example.com"
        );
    }

    @Test
    void shouldConfigureAllowedMethods() {

        CorsConfiguration configuration =
                configuration();

        assertThat(
                configuration.getAllowedMethods()
        ).containsExactly(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        );
    }

    @Test
    void shouldConfigureAllowedHeaders() {

        CorsConfiguration configuration =
                configuration();

        assertThat(
                configuration.getAllowedHeaders()
        ).containsExactly(
                "Content-Type",
                "Accept",
                "X-XSRF-TOKEN",
                "X-Correlation-Id"
        );
    }

    @Test
    void shouldExposeCorrelationIdHeader() {

        CorsConfiguration configuration =
                configuration();

        assertThat(
                configuration.getExposedHeaders()
        ).containsExactly(
                "X-Correlation-Id"
        );
    }

    @Test
    void shouldAllowCredentials() {

        CorsConfiguration configuration =
                configuration();

        assertThat(
                configuration.getAllowCredentials()
        ).isTrue();
    }

    @Test
    void shouldConfigureMaxAge() {

        CorsConfiguration configuration =
                configuration();

        assertThat(
                configuration.getMaxAge()
        ).isEqualTo(
                3600L
        );
    }

    @Test
    void shouldApplyCorsConfigurationToAllPaths() {

        UrlBasedCorsConfigurationSource source =
                corsConfig.corsConfigurationSource(
                        List.of(
                                "https://localhost"
                        )
                );

        assertThat(
                source.getCorsConfiguration(
                        exchange("/api/events")
                )
        ).isNotNull();

        assertThat(
                source.getCorsConfiguration(
                        exchange("/api/reservations")
                )
        ).isNotNull();

        assertThat(
                source.getCorsConfiguration(
                        exchange("/api/payments")
                )
        ).isNotNull();

        assertThat(
                source.getCorsConfiguration(
                        exchange("/oauth2/authorization/cloud-bff")
                )
        ).isNotNull();
    }

    private CorsConfiguration configuration() {

        UrlBasedCorsConfigurationSource source =
                corsConfig.corsConfigurationSource(
                        List.of(
                                "https://localhost"
                        )
                );

        return requireNonNull(
                source.getCorsConfiguration(
                        exchange("/api/events")
                )
        );
    }

    private MockServerWebExchange exchange(
            String path
    ) {

        return MockServerWebExchange.from(
                MockServerHttpRequest
                        .get(path)
                        .build()
        );
    }
}