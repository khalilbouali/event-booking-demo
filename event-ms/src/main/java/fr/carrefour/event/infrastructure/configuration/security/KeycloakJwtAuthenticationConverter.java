package fr.carrefour.event.infrastructure.configuration.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;

import static java.util.Collections.emptyList;
import static reactor.core.publisher.Mono.just;

@Component
public class KeycloakJwtAuthenticationConverter
        implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    private final String clientId;

    public KeycloakJwtAuthenticationConverter(
            @Value("${security.oauth2.client-id}")
            String clientId
    ) {
        this.clientId = clientId;
    }

    @Override
    public Mono<AbstractAuthenticationToken> convert(
            Jwt jwt
    ) {

        Collection<GrantedAuthority> authorities =
                createResourceAccessRoles(
                        jwt,
                        clientId
                );

        return just(
                new JwtAuthenticationToken(
                        jwt,
                        authorities
                )
        );
    }

    private Collection<GrantedAuthority> createResourceAccessRoles(
            Jwt jwt,
            String clientId
    ) {

        Map<String, Object> resourceAccess =
                jwt.getClaim("resource_access");

        if (resourceAccess == null) {
            return emptyList();
        }

        Object clientAccessObject =
                resourceAccess.get(clientId);

        if (!(clientAccessObject instanceof Map<?, ?> clientAccess)) {
            return emptyList();
        }

        Object rolesObject =
                clientAccess.get("roles");

        if (!(rolesObject instanceof Collection<?> roles)) {
            return emptyList();
        }

        return roles.stream()
                .map(Object::toString)
                .map(role -> "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
