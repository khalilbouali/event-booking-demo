package fr.carrefour.reservation.infrastructure.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.web.reactive.function.client.ServerBearerExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.web.reactive.function.client.WebClient.builder;


@Configuration
public class EventWebClientConfiguration {

    @Bean
    WebClient eventWebClient(
            @Value("${services.event.base-url}")
            String eventBaseUrl
    ) {

        ServerBearerExchangeFilterFunction bearerToken =
                new ServerBearerExchangeFilterFunction();

        return builder()
                .baseUrl(eventBaseUrl)
                .filter(bearerToken)
                .build();
    }
}
