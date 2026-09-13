package fr.carrefour.bff.infrastructure.configuration.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

class AccessTokenRoleExtractorTest {

    private static final String TOKEN =
            "access-token";

    private static final String CLIENT_ID =
            "carrefour-kata-id";

    private ReactiveJwtDecoder jwtDecoder;

    private Jwt jwt;

    private AccessTokenRoleExtractor roleExtractor;

    @BeforeEach
    void setUp() {

        jwtDecoder =
                mock(ReactiveJwtDecoder.class);

        jwt =
                mock(Jwt.class);

        roleExtractor =
                new AccessTokenRoleExtractor(
                        jwtDecoder
                );

        when(
                jwtDecoder.decode(TOKEN)
        ).thenReturn(
                Mono.just(jwt)
        );
    }

    @Test
    void shouldReturnTrueWhenClientContainsExpectedRole() {

        Map<String, Object> resourceAccess =
                Map.of(
                        CLIENT_ID,
                        Map.of(
                                "roles",
                                List.of(
                                        "user",
                                        "administrator"
                                )
                        )
                );

        doReturn(resourceAccess)
                .when(jwt)
                .getClaim("resource_access");

        StepVerifier
                .create(
                        roleExtractor.hasClientRole(
                                TOKEN,
                                CLIENT_ID,
                                "administrator"
                        )
                )
                .expectNext(true)
                .verifyComplete();

        verify(
                jwtDecoder
        ).decode(TOKEN);
    }

    @Test
    void shouldMatchRoleIgnoringCase() {

        Map<String, Object> resourceAccess =
                Map.of(
                        CLIENT_ID,
                        Map.of(
                                "roles",
                                List.of(
                                        "ADMINISTRATOR"
                                )
                        )
                );

        doReturn(resourceAccess)
                .when(jwt)
                .getClaim("resource_access");

        StepVerifier
                .create(
                        roleExtractor.hasClientRole(
                                TOKEN,
                                CLIENT_ID,
                                "administrator"
                        )
                )
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenExpectedRoleIsMissing() {

        Map<String, Object> resourceAccess =
                Map.of(
                        CLIENT_ID,
                        Map.of(
                                "roles",
                                List.of(
                                        "user"
                                )
                        )
                );

        doReturn(resourceAccess)
                .when(jwt)
                .getClaim("resource_access");

        StepVerifier
                .create(
                        roleExtractor.hasClientRole(
                                TOKEN,
                                CLIENT_ID,
                                "administrator"
                        )
                )
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenResourceAccessIsMissing() {

        doReturn(null)
                .when(jwt)
                .getClaim("resource_access");

        StepVerifier
                .create(
                        roleExtractor.hasClientRole(
                                TOKEN,
                                CLIENT_ID,
                                "administrator"
                        )
                )
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenClientIsMissing() {

        Map<String, Object> resourceAccess =
                Map.of(
                        "another-client",
                        Map.of(
                                "roles",
                                List.of(
                                        "administrator"
                                )
                        )
                );

        doReturn(resourceAccess)
                .when(jwt)
                .getClaim("resource_access");

        StepVerifier
                .create(
                        roleExtractor.hasClientRole(
                                TOKEN,
                                CLIENT_ID,
                                "administrator"
                        )
                )
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenClientAccessIsNotAMap() {

        Map<String, Object> resourceAccess =
                Map.of(
                        CLIENT_ID,
                        "invalid-client-access"
                );

        doReturn(resourceAccess)
                .when(jwt)
                .getClaim("resource_access");

        StepVerifier
                .create(
                        roleExtractor.hasClientRole(
                                TOKEN,
                                CLIENT_ID,
                                "administrator"
                        )
                )
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenRolesClaimIsMissing() {

        Map<String, Object> resourceAccess =
                Map.of(
                        CLIENT_ID,
                        Map.of(
                                "some-other-claim",
                                "value"
                        )
                );

        doReturn(resourceAccess)
                .when(jwt)
                .getClaim("resource_access");

        StepVerifier
                .create(
                        roleExtractor.hasClientRole(
                                TOKEN,
                                CLIENT_ID,
                                "administrator"
                        )
                )
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenRolesClaimIsNotACollection() {

        Map<String, Object> resourceAccess =
                Map.of(
                        CLIENT_ID,
                        Map.of(
                                "roles",
                                "administrator"
                        )
                );

        doReturn(resourceAccess)
                .when(jwt)
                .getClaim("resource_access");

        StepVerifier
                .create(
                        roleExtractor.hasClientRole(
                                TOKEN,
                                CLIENT_ID,
                                "administrator"
                        )
                )
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldPropagateJwtDecoderFailure() {

        RuntimeException failure =
                new RuntimeException(
                        "Token decoding failed"
                );

        when(
                jwtDecoder.decode(TOKEN)
        ).thenReturn(
                Mono.error(failure)
        );

        StepVerifier
                .create(
                        roleExtractor.hasClientRole(
                                TOKEN,
                                CLIENT_ID,
                                "administrator"
                        )
                )
                .expectErrorSatisfies(error ->
                        org.assertj.core.api.Assertions
                                .assertThat(error)
                                .isSameAs(failure)
                )
                .verify();
    }
}