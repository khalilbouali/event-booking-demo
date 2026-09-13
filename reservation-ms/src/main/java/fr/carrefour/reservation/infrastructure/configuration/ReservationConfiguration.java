package fr.carrefour.reservation.infrastructure.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

import static java.time.Clock.systemUTC;

@Configuration
@EnableConfigurationProperties(ReservationProperties.class)
public class ReservationConfiguration {

    @Bean
    Clock clock() {
        return systemUTC();
    }

    @Bean
    @Qualifier("reservationHoldDuration")
    Duration reservationHoldDuration(
            ReservationProperties properties
    ) {
        return properties.holdDuration();
    }
}