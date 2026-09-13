package fr.carrefour.payment.domain.exception;

import java.math.BigDecimal;

public class InvalidPaymentAmountException extends RuntimeException {

    public InvalidPaymentAmountException(BigDecimal amount) {
        super("Payment amount must be greater than zero: " + amount);
    }
}