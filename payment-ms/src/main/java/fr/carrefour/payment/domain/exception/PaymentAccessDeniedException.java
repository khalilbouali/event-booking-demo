package fr.carrefour.payment.domain.exception;

import java.util.UUID;

public class PaymentAccessDeniedException extends RuntimeException {

    public PaymentAccessDeniedException(UUID paymentId) {
        super("Access denied to payment: " + paymentId);
    }
}
