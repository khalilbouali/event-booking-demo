package fr.carrefour.reservation.infrastructure.adapter.in.web;

import fr.carrefour.reservation.application.port.in.GetReservationUseCase;
import fr.carrefour.reservation.application.port.in.HoldSeatUseCase;
import fr.carrefour.reservation.application.port.in.ListMyReservationsUseCase;
import fr.carrefour.reservation.infrastructure.adapter.in.web.dto.HoldSeatRequest;
import fr.carrefour.reservation.infrastructure.adapter.in.web.dto.ReservationResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final HoldSeatUseCase holdSeatUseCase;
    private final GetReservationUseCase getReservationUseCase;
    private final ListMyReservationsUseCase listMyReservationsUseCase;

    public ReservationController(
            HoldSeatUseCase holdSeatUseCase,
            GetReservationUseCase getReservationUseCase,
            ListMyReservationsUseCase listMyReservationsUseCase) {

        this.holdSeatUseCase = holdSeatUseCase;
        this.getReservationUseCase = getReservationUseCase;
        this.listMyReservationsUseCase = listMyReservationsUseCase;
    }

    @PostMapping
    public Mono<ResponseEntity<ReservationResponse>> hold(
            @Valid @RequestBody HoldSeatRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String customerId = jwt.getSubject();

        return holdSeatUseCase
                .hold(
                        request.eventId(),
                        customerId
                )
                .map(ReservationResponse::from)
                .map(response ->
                        ResponseEntity
                                .status(CREATED)
                                .body(response)
                );
    }

    @GetMapping("/{id}")
    public Mono<ReservationResponse> get(
            @PathVariable UUID id) {

        return getReservationUseCase
                .getById(id)
                .map(ReservationResponse::from);
    }

    @GetMapping
    public Flux<ReservationResponse> findMine(
            @AuthenticationPrincipal Jwt jwt
    ) {

        String customerId = jwt.getSubject();

        return listMyReservationsUseCase
                .findByCustomerId(customerId)
                .map(ReservationResponse::from);
    }
}