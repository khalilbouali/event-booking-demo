package fr.carrefour.event.infrastructure.adapter.out.persistence.mongodb.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;
import java.util.UUID;

@Document("event_availability")
public record EventAvailabilityDocument(

        @Id
        UUID eventId,

        int totalSeats,

        int remainingSeats,

        Set<String> blockedSeatIds
) {
}
