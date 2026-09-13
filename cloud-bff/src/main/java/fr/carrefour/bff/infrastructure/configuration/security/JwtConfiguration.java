package fr.carrefour.bff.infrastructure.configuration.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import static org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder.withJwkSetUri;

@Configuration
public class JwtConfiguration {

    @Bean
    ReactiveJwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.client.provider.keycloak.jwk-set-uri}")
            String jwkSetUri
    ) {

        return withJwkSetUri(jwkSetUri)
                .build();
    }
}
