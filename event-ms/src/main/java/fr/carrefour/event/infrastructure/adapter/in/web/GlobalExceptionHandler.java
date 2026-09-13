package fr.carrefour.event.infrastructure.adapter.in.web;

import fr.carrefour.event.domain.exception.DuplicateSeatException;
import fr.carrefour.event.domain.exception.EventNotFoundException;
import fr.carrefour.event.domain.exception.InvalidEventException;
import fr.carrefour.event.domain.exception.InvalidSeatException;
import fr.carrefour.event.infrastructure.adapter.in.web.dto.ApiError;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import static java.time.Instant.now;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.status;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            EventNotFoundException exception
    ) {
        return status(NOT_FOUND)
                .body(new ApiError(
                        NOT_FOUND.value(),
                        exception.getMessage(),
                        now()
                ));
    }

    @ExceptionHandler({
            InvalidEventException.class,
            InvalidSeatException.class,
            DuplicateSeatException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(
            RuntimeException exception
    ) {
        return badRequest()
                .body(new ApiError(
                        BAD_REQUEST.value(),
                        exception.getMessage(),
                        now()
                ));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiError> handleValidation(
            WebExchangeBindException exception
    ) {
        String message = exception.getFieldErrors()
                .stream()
                .findFirst()
                .map(error ->
                        error.getField()
                                + ": "
                                + error.getDefaultMessage()
                )
                .orElse("Invalid request");

        return badRequest()
                .body(new ApiError(
                        BAD_REQUEST.value(),
                        message,
                        now()
                ));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleMethodValidation(
            HandlerMethodValidationException exception
    ) {

        String message = exception
                .getAllErrors()
                .stream()
                .findFirst()
                .map(MessageSourceResolvable::getDefaultMessage)
                .orElse("Invalid request");

        return badRequest()
                .body(new ApiError(
                        BAD_REQUEST.value(),
                        message,
                        now()
                ));
    }
}
