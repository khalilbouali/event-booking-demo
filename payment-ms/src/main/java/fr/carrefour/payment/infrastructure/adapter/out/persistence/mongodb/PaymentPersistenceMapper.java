package fr.carrefour.payment.infrastructure.adapter.out.persistence.mongodb;

import fr.carrefour.payment.domain.model.Payment;
import org.springframework.stereotype.Component;

import static fr.carrefour.payment.domain.model.Payment.restore;

@Component
public class PaymentPersistenceMapper {

    public PaymentDocument toDocument(Payment payment) {
        return new PaymentDocument(
                payment.getId(),
                payment.getReservationId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }

    public Payment toDomain(PaymentDocument document) {
        return restore(
                document.getId(),
                document.getReservationId(),
                document.getCustomerId(),
                document.getAmount(),
                document.getStatus(),
                document.getCreatedAt()
        );
    }
}