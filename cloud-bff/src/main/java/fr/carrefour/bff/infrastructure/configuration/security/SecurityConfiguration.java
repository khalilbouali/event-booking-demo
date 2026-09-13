package fr.carrefour.bff.infrastructure.configuration.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.WebSessionServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.*;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.CsrfServerLogoutHandler;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository.withHttpOnlyFalse;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ServerLogoutSuccessHandler oidcLogoutSuccessHandler,
            ServerOAuth2AuthorizedClientRepository authorizedClientRepository,
            ServerCsrfTokenRepository csrfTokenRepository,
            ServerLogoutHandler logoutHandler
    ) {

        ServerCsrfTokenRequestAttributeHandler csrfRequestHandler =
                new ServerCsrfTokenRequestAttributeHandler();

        return http

                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(csrfRequestHandler)
                )
                .cors(withDefaults())
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(
                                "/oauth2/**",
                                "/login/**"
                        ).permitAll()
                        .pathMatchers(
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().permitAll()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                (exchange, exception1) -> {

                                    exchange.getResponse()
                                            .setStatusCode(
                                                    UNAUTHORIZED
                                            );

                                    return exchange.getResponse()
                                            .setComplete();
                                }
                        )
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizedClientRepository(authorizedClientRepository)
                )
                .oauth2Client(oauth2 -> oauth2
                        .authorizedClientRepository(authorizedClientRepository)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutHandler(logoutHandler)
                        .logoutSuccessHandler(oidcLogoutSuccessHandler)
                )
                .build();
    }

    @Bean
    ServerOAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new WebSessionServerOAuth2AuthorizedClientRepository();
    }

    @Bean
    CookieServerCsrfTokenRepository csrfTokenRepository() {
        return withHttpOnlyFalse();
    }

    @Bean
    ServerLogoutHandler logoutHandler(
            CookieServerCsrfTokenRepository csrfTokenRepository
    ) {
        return new DelegatingServerLogoutHandler(
                new SecurityContextServerLogoutHandler(),
                new CsrfServerLogoutHandler(csrfTokenRepository),
                new WebSessionServerLogoutHandler()
        );
    }
}
