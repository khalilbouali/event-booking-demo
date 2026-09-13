package fr.carrefour.reservation.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record HoldSeatRequest(

        @NotNull
        UUID eventId

) {
}