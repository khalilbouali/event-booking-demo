package fr.carrefour.reservation.application.model;

import java.util.List;
import java.util.UUID;

public record EventSeatCandidates(
        UUID eventId,
        List<String> seatIds
) {
}
