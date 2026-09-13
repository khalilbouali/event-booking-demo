package fr.carrefour.event.infrastructure.adapter.in.web;

import java.util.List;
import java.util.UUID;

public record EventSeatCandidatesResponse(
        UUID eventId,
        List<String> seatIds
) {
}