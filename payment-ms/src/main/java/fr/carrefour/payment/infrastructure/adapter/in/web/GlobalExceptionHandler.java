package fr.carrefour.payment.infrastructure.adapter.in.web;

import fr.carrefour.payment.domain.exception.*;
import fr.carrefour.payment.infrastructure.adapter.in.web.dto.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import static java.time.Instant.now;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.status;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            PaymentNotFoundException exception
    ) {
        return status(NOT_FOUND)
                .body(new ApiError(
                        NOT_FOUND.value(),
                        exception.getMessage(),
                        now()
                ));
    }

    @ExceptionHandler({
            InvalidPaymentAmountException.class,
            InvalidCustomerIdException.class
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

    @ExceptionHandler(InvalidPaymentStateException.class)
    public ResponseEntity<ApiError> handleConflict(
            InvalidPaymentStateException exception
    ) {
        return status(CONFLICT)
                .body(new ApiError(
                        CONFLICT.value(),
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
                        error.getField() + ": " + error.getDefaultMessage()
                )
                .orElse("Invalid request");

        return badRequest()
                .body(new ApiError(
                        BAD_REQUEST.value(),
                        message,
                        now()
                ));
    }

    @ExceptionHandler(PaymentAccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            PaymentAccessDeniedException exception
    ) {
        return status(FORBIDDEN)
                .body(new ApiError(
                        FORBIDDEN.value(),
                        exception.getMessage(),
                        now()
                ));
    }
}
