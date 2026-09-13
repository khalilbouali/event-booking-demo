package fr.carrefour.reservation.infrastructure.adapter.out.persistence.mongodb;

import fr.carrefour.reservation.domain.model.ReservationStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reservations")
@CompoundIndexes({

        @CompoundIndex(
                name = "uq_active_reservation_per_seat",
                def = "{'messageId': 1, 'seatId': 1}",
                unique = true,
                partialFilter = "{'active': true}"
        ),

        @CompoundIndex(
                name = "idx_status_expires_at",
                def = "{'status': 1, 'expiresAt': 1}"
        )
})
public class ReservationDocument {

    @Id
    private UUID id;

    private UUID eventId;
    private String seatId;
    private String customerId;

    private ReservationStatus status;

    private Instant createdAt;
    private Instant expiresAt;

    private boolean active;
}