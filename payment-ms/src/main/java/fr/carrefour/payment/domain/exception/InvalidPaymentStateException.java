package fr.carrefour.payment.domain.exception;

import fr.carrefour.payment.domain.model.PaymentStatus;

public class InvalidPaymentStateException extends RuntimeException {

    public InvalidPaymentStateException(
            PaymentStatus currentStatus,
            PaymentStatus targetStatus
    ) {
        super(
                "Cannot transition payment from "
                        + currentStatus
                        + " to "
                        + targetStatus
        );
    }
}