package fr.carrefour.payment.infrastructure.adapter.out.persistence.mongodb;

import fr.carrefour.payment.domain.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payments")
@CompoundIndex(
        name = "idx_customer_created_at",
        def = "{'customerId': 1, 'createdAt': -1}"
)
public class PaymentDocument {

    @Id
    private UUID id;

    private UUID reservationId;

    private String customerId;

    private BigDecimal amount;

    private PaymentStatus status;

    private Instant createdAt;
}
