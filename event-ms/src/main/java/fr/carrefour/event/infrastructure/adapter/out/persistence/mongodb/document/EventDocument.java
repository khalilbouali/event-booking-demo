package fr.carrefour.event.infrastructure.adapter.out.persistence.mongodb.document;

import fr.carrefour.event.domain.model.Seat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "events")
public class EventDocument {

    @Id
    private UUID id;

    private String name;

    private String venue;

    private Instant startsAt;

    private List<Seat> seats;

    private BigDecimal price;
}