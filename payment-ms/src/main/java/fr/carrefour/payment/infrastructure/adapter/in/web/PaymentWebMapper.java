package fr.carrefour.payment.infrastructure.adapter.in.web;

import fr.carrefour.payment.domain.model.Payment;
import fr.carrefour.payment.infrastructure.adapter.in.web.dto.PaymentResponse;

final class PaymentWebMapper {

    private PaymentWebMapper() {
    }

    static PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getReservationId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }
}
