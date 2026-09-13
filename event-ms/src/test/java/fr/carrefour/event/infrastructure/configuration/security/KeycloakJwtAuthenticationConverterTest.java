package fr.carrefour.event.infrastructure.configuration.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtAuthenticationConverterTest {

    private static final String CLIENT_ID =
            "carrefour-kata-id";

    private KeycloakJwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        converter =
                new KeycloakJwtAuthenticationConverter(
                        CLIENT_ID
                );
    }

    @Test
    void shouldConvertClientRolesToSpringAuthorities() {

        Jwt jwt =
                jwt(
                        Map.of(
                                "resource_access",
                                Map.of(
                                        CLIENT_ID,
                                        Map.of(
                                                "roles",
                                                List.of(
                                                        "administrator",
                                                        "user"
                                                )
                                        )
                                )
                        )
                );

        StepVerifier
                .create(
                        converter.convert(jwt)
                )
                .assertNext(authentication -> {

                    assertThat(authentication)
                            .isInstanceOf(
                                    JwtAuthenticationToken.class
                            );

                    assertThat(
                            authentication.getAuthorities()
                    )
                            .extracting(
                                    "authority"
                            )
                            .containsExactlyInAnyOrder(
                                    "ROLE_administrator",
                                    "ROLE_user"
                            );
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnNoAuthoritiesWhenResourceAccessIsMissing() {

        Jwt jwt =
                jwt(
                        Map.of()
                );

        StepVerifier
                .create(
                        converter.convert(jwt)
                )
                .assertNext(authentication ->
                        assertThat(
                                authentication.getAuthorities()
                        ).isEmpty()
                )
                .verifyComplete();
    }

    @Test
    void shouldReturnNoAuthoritiesWhenClientIsMissing() {

        Jwt jwt =
                jwt(
                        Map.of(
                                "resource_access",
                                Map.of(
                                        "another-client",
                                        Map.of(
                                                "roles",
                                                List.of(
                                                        "user"
                                                )
                                        )
                                )
                        )
                );

        StepVerifier
                .create(
                        converter.convert(jwt)
                )
                .assertNext(authentication ->
                        assertThat(
                                authentication.getAuthorities()
                        ).isEmpty()
                )
                .verifyComplete();
    }

    @Test
    void shouldReturnNoAuthoritiesWhenClientAccessIsNotAMap() {

        Jwt jwt =
                jwt(
                        Map.of(
                                "resource_access",
                                Map.of(
                                        CLIENT_ID,
                                        "invalid"
                                )
                        )
                );

        StepVerifier
                .create(
                        converter.convert(jwt)
                )
                .assertNext(authentication ->
                        assertThat(
                                authentication.getAuthorities()
                        ).isEmpty()
                )
                .verifyComplete();
    }

    @Test
    void shouldReturnNoAuthoritiesWhenRolesAreMissing() {

        Jwt jwt =
                jwt(
                        Map.of(
                                "resource_access",
                                Map.of(
                                        CLIENT_ID,
                                        Map.of()
                                )
                        )
                );

        StepVerifier
                .create(
                        converter.convert(jwt)
                )
                .assertNext(authentication ->
                        assertThat(
                                authentication.getAuthorities()
                        ).isEmpty()
                )
                .verifyComplete();
    }

    @Test
    void shouldReturnNoAuthoritiesWhenRolesAreNotACollection() {

        Jwt jwt =
                jwt(
                        Map.of(
                                "resource_access",
                                Map.of(
                                        CLIENT_ID,
                                        Map.of(
                                                "roles",
                                                "user"
                                        )
                                )
                        )
                );

        StepVerifier
                .create(
                        converter.convert(jwt)
                )
                .assertNext(authentication ->
                        assertThat(
                                authentication.getAuthorities()
                        ).isEmpty()
                )
                .verifyComplete();
    }

    @Test
    void shouldReturnAuthenticationContainingOriginalJwt() {

        Jwt jwt =
                jwt(
                        Map.of(
                                "resource_access",
                                Map.of(
                                        CLIENT_ID,
                                        Map.of(
                                                "roles",
                                                List.of(
                                                        "user"
                                                )
                                        )
                                )
                        )
                );

        StepVerifier
                .create(
                        converter.convert(jwt)
                )
                .assertNext(authentication -> {

                    JwtAuthenticationToken token =
                            (JwtAuthenticationToken)
                                    authentication;

                    assertThat(
                            token.getToken()
                    ).isSameAs(
                            jwt
                    );

                    assertThat(
                            token.getAuthorities()
                    )
                            .extracting(
                                    "authority"
                            )
                            .containsExactly(
                                    "ROLE_user"
                            );
                })
                .verifyComplete();
    }

    private Jwt jwt(
            Map<String, Object> claims
    ) {

        Jwt.Builder builder =
                Jwt.withTokenValue(
                                "test-token"
                        )
                        .header(
                                "alg",
                                "none"
                        )
                        .subject(
                                "user-123"
                        );

        claims.forEach(
                builder::claim
        );

        return builder.build();
    }
}