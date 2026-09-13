package fr.carrefour.reservation.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "reservation")
public record ReservationProperties(
        Duration holdDuration,
        Duration expirationCheckDelay
) {
}