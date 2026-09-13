package fr.carrefour.bff.infrastructure.configuration.security;

import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;

@Component
public class AccessTokenRoleExtractor {

    private final ReactiveJwtDecoder jwtDecoder;

    public AccessTokenRoleExtractor(
            ReactiveJwtDecoder jwtDecoder
    ) {
        this.jwtDecoder = jwtDecoder;
    }

    public Mono<Boolean> hasClientRole(
            String token,
            String clientId,
            String expectedRole
    ) {

        return jwtDecoder
                .decode(token)
                .map(jwt -> {

                    Map<String, Object> resourceAccess =
                            jwt.getClaim("resource_access");

                    if (resourceAccess == null) {
                        return false;
                    }

                    Object clientAccessObject =
                            resourceAccess.get(clientId);

                    if (!(clientAccessObject instanceof Map<?, ?> clientAccess)) {
                        return false;
                    }

                    Object rolesObject =
                            clientAccess.get("roles");

                    if (!(rolesObject instanceof Collection<?> roles)) {
                        return false;
                    }

                    return roles.stream()
                            .map(Object::toString)
                            .anyMatch(role ->
                                    role.equalsIgnoreCase(expectedRole)
                            );
                });
    }
}
