package fr.carrefour.bff.infrastructure.configuration;

import fr.carrefour.bff.infrastructure.configuration.security.RefreshRotationAuthorizationSuccessHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client
        .ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.session.WebSessionManager;
import reactor.netty.http.client.HttpClient;

import java.util.Arrays;

import static fr.carrefour.bff.infrastructure.filter.CorrelationIdFilter.CONTEXT_KEY;
import static fr.carrefour.bff.infrastructure.filter.CorrelationIdFilter.HEADER_NAME;
import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.web.reactive.function.client.ClientRequest.from;
import static org.springframework.web.reactive.function.client.WebClient.builder;
import static reactor.core.publisher.Mono.deferContextual;
import static reactor.netty.http.client.HttpClient.create;

@Configuration
public class WebClientConfiguration {

    @Bean
    ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ServerOAuth2AuthorizedClientRepository authorizedClientRepository,
            RefreshRotationAuthorizationSuccessHandler successHandler
    ) {

        ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider =
                ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                        .authorizationCode()
                        .refreshToken()
                        .build();

        DefaultReactiveOAuth2AuthorizedClientManager authorizedClientManager =
                new DefaultReactiveOAuth2AuthorizedClientManager(
                        clientRegistrationRepository,
                        authorizedClientRepository
                );

        authorizedClientManager.setAuthorizedClientProvider(
                authorizedClientProvider
        );

        authorizedClientManager.setAuthorizationSuccessHandler(
                successHandler
        );

        return authorizedClientManager;
    }

    @Bean
    WebClient microserviceWebClient(
            ReactiveOAuth2AuthorizedClientManager authorizedClientManager
    ) {

        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(
                        authorizedClientManager
                );

        oauth2.setDefaultOAuth2AuthorizedClient(true);

        HttpClient httpClient =
                create()
                        .option(
                                CONNECT_TIMEOUT_MILLIS,
                                3000
                        )
                        .responseTimeout(
                                ofSeconds(5)
                        )
                        .doOnConnected(connection ->
                                connection
                                        .addHandlerLast(
                                                new ReadTimeoutHandler(
                                                        5,
                                                        SECONDS
                                                )
                                        )
                                        .addHandlerLast(
                                                new WriteTimeoutHandler(
                                                        5,
                                                        SECONDS
                                                )
                                        )
                        );

        return builder()
                .clientConnector(
                        new ReactorClientHttpConnector(httpClient)
                )
                .filter(correlationIdFilter())
                .filter(oauth2)
                .build();
    }

    private ExchangeFilterFunction correlationIdFilter() {

        return (request, next) ->
                deferContextual(contextView -> {

                    if (!contextView.hasKey(CONTEXT_KEY)) {
                        return next.exchange(request);
                    }

                    String correlationId =
                            contextView.get(CONTEXT_KEY);

                    ClientRequest correlatedRequest =
                            from(request)
                                    .header(
                                            HEADER_NAME,
                                            correlationId
                                    )
                                    .build();

                    return next.exchange(
                            correlatedRequest
                    );
                });
    }
}
