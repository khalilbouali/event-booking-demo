package fr.carrefour.reservation.infrastructure.adapter.in.web;

import fr.carrefour.reservation.domain.exception.NoAvailableSeatException;
import fr.carrefour.reservation.domain.exception.ReservationNotFoundException;
import fr.carrefour.reservation.domain.exception.SeatUnavailableException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.ProblemDetail.forStatusAndDetail;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SeatUnavailableException.class)
    public ProblemDetail handleSeatUnavailable(
            SeatUnavailableException exception) {

        ProblemDetail problem = forStatusAndDetail(
                CONFLICT,
                exception.getMessage()
        );

        problem.setTitle("Seat unavailable");

        return problem;
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ProblemDetail handleReservationNotFound(
            ReservationNotFoundException exception) {

        ProblemDetail problem = forStatusAndDetail(
                NOT_FOUND,
                exception.getMessage()
        );

        problem.setTitle("Reservation not found");

        return problem;
    }

    @ExceptionHandler(NoAvailableSeatException.class)
    public ResponseEntity<Void> handleNoAvailableSeat(
            NoAvailableSeatException exception
    ) {

        return ResponseEntity
                .status(CONFLICT)
                .build();
    }
}