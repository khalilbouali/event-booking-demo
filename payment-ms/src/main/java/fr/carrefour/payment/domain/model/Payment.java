package fr.carrefour.payment.domain.model;

import fr.carrefour.payment.domain.exception.InvalidCustomerIdException;
import fr.carrefour.payment.domain.exception.InvalidPaymentAmountException;
import fr.carrefour.payment.domain.exception.InvalidPaymentStateException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static fr.carrefour.payment.domain.model.PaymentStatus.*;
import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;

@Getter
public class Payment {

    private final UUID id;
    private final UUID reservationId;
    private final String customerId;
    private final BigDecimal amount;
    private final Instant createdAt;

    private PaymentStatus status;

    private Payment(
            UUID id,
            UUID reservationId,
            String customerId,
            BigDecimal amount,
            PaymentStatus status,
            Instant createdAt
    ) {
        this.id = requireNonNull(id, "Payment id must not be null");
        this.reservationId = requireNonNull(
                reservationId,
                "Reservation id must not be null"
        );
        this.customerId = requireCustomerId(customerId);
        this.amount = requireValidAmount(amount);
        this.status = requireNonNull(
                status,
                "Payment status must not be null"
        );
        this.createdAt = requireNonNull(
                createdAt,
                "Created at must not be null"
        );
    }

    public static Payment create(
            UUID reservationId,
            String customerId,
            BigDecimal amount,
            Instant now,
            boolean isValid
    ) {
        return new Payment(
                randomUUID(),
                reservationId,
                customerId,
                amount,
                isValid ? SUCCEEDED : FAILED,
                now
        );
    }

    public static Payment restore(
            UUID id,
            UUID reservationId,
            String customerId,
            BigDecimal amount,
            PaymentStatus status,
            Instant createdAt
    ) {
        return new Payment(
                id,
                reservationId,
                customerId,
                amount,
                status,
                createdAt
        );
    }

    public void succeed() {
        if (status == SUCCEEDED) {
            return;
        }

        if (status != PENDING) {
            throw new InvalidPaymentStateException(
                    status,
                    SUCCEEDED
            );
        }

        status = SUCCEEDED;
    }

    public void fail() {
        if (status == FAILED) {
            return;
        }

        if (status != PENDING) {
            throw new InvalidPaymentStateException(
                    status,
                    FAILED
            );
        }

        status = FAILED;
    }

    private static BigDecimal requireValidAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidPaymentAmountException(amount);
        }

        return amount;
    }

    private static String requireCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new InvalidCustomerIdException();
        }

        return customerId;
    }
}