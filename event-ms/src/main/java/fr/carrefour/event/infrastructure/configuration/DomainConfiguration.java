package fr.carrefour.event.infrastructure.configuration;

import fr.carrefour.event.domain.service.SeatLayoutGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfiguration {

    @Bean
    SeatLayoutGenerator seatLayoutGenerator() {
        return new SeatLayoutGenerator();
    }
}
