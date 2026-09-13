package fr.carrefour.reservation.infrastructure.adapter.out.event.dto;

import java.util.List;
import java.util.UUID;

public record EventSeatCandidatesResponse(
        UUID eventId,
        List<String> seatIds
) {
}
