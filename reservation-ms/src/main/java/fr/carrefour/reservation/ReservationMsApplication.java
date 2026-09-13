package fr.carrefour.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ReservationMsApplication {

	static void main(String[] args) {
		SpringApplication.run(ReservationMsApplication.class, args);
	}

}
