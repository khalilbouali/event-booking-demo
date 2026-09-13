package fr.carrefour.reservation.infrastructure.adapter.out.persistence.mongodb;

import fr.carrefour.reservation.domain.model.Reservation;
import org.springframework.stereotype.Component;

import static fr.carrefour.reservation.domain.model.Reservation.restore;

@Component
public class ReservationPersistenceMapper {

    public ReservationDocument toDocument(Reservation reservation) {

        return new ReservationDocument(
                reservation.getId(),
                reservation.getEventId(),
                reservation.getSeatId(),
                reservation.getCustomerId(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getExpiresAt(),
                reservation.blocksSeatAvailability()
        );
    }

    public Reservation toDomain(ReservationDocument document) {

        return restore(
                document.getId(),
                document.getEventId(),
                document.getSeatId(),
                document.getCustomerId(),
                document.getStatus(),
                document.getCreatedAt(),
                document.getExpiresAt()
        );
    }
}