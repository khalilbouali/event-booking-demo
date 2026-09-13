package fr.carrefour.event.application.model;

import java.util.List;
import java.util.UUID;

public record EventSeatCandidates(
        UUID eventId,
        List<String> seatIds
) {
}