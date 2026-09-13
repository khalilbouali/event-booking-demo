package fr.carrefour.reservation.application.port.in;

import fr.carrefour.reservation.domain.model.Reservation;
import reactor.core.publisher.Flux;

public interface ListMyReservationsUseCase {

    Flux<Reservation> findByCustomerId(String customerId);
}