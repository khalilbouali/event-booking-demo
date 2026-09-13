package fr.carrefour.payment.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

import static java.time.Clock.systemUTC;

@Configuration
public class PaymentConfiguration {

    @Bean
    Clock clock() {
        return systemUTC();
    }
}
