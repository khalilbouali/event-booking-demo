package fr.carrefour.payment;

import static org.springframework.boot.SpringApplication.from;

public class TestPaymentMsApplication {

	static void main(String[] args) {
		from(PaymentMsApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
