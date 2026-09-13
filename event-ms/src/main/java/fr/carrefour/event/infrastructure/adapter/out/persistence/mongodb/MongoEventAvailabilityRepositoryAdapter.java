package fr.carrefour.event.infrastructure.adapter.out.persistence.mongodb;

import fr.carrefour.event.application.port.out.EventAvailabilityRepository;
import fr.carrefour.event.domain.model.EventAvailability;
import fr.carrefour.event.infrastructure.adapter.out.persistence.mongodb.document.EventAvailabilityDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;

import static java.util.Set.copyOf;
import static java.util.Set.of;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Component
@RequiredArgsConstructor
public class MongoEventAvailabilityRepositoryAdapter
        implements EventAvailabilityRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    @Override
    public Mono<Void> initialize(
            UUID eventId,
            int totalSeats
    ) {

        Query query = query(
                where("_id")
                        .is(eventId)
        );

        Update update = new Update()
                .setOnInsert(
                        "totalSeats",
                        totalSeats
                )
                .setOnInsert(
                        "remainingSeats",
                        totalSeats
                )
                .setOnInsert(
                        "blockedSeatIds",
                        of()
                );

        return mongoTemplate
                .upsert(
                        query,
                        update,
                        EventAvailabilityDocument.class
                )
                .then();
    }

    @Override
    public Mono<Void> blockSeat(
            UUID eventId,
            String seatId
    ) {

        Query query = query(
                where("_id")
                        .is(eventId)
                        .and("remainingSeats")
                        .gt(0)
                        .and("blockedSeatIds")
                        .ne(seatId)
        );

        Update update = new Update()
                .addToSet(
                        "blockedSeatIds",
                        seatId
                )
                .inc(
                        "remainingSeats",
                        -1
                );

        return mongoTemplate
                .updateFirst(
                        query,
                        update,
                        EventAvailabilityDocument.class
                )
                .then();
    }

    @Override
    public Mono<Void> releaseSeat(
            UUID eventId,
            String seatId
    ) {

        Query query = query(
                where("_id")
                        .is(eventId)
                        .and("blockedSeatIds")
                        .is(seatId)
        );

        Update update = new Update()
                .pull(
                        "blockedSeatIds",
                        seatId
                )
                .inc(
                        "remainingSeats",
                        1
                );

        return mongoTemplate
                .updateFirst(
                        query,
                        update,
                        EventAvailabilityDocument.class
                )
                .then();
    }

    @Override
    public Mono<EventAvailability> findByEventId(
            UUID eventId
    ) {

        return mongoTemplate
                .findById(
                        eventId,
                        EventAvailabilityDocument.class
                )
                .map(document ->
                        new EventAvailability(
                                document.eventId(),
                                document.totalSeats(),
                                document.remainingSeats()
                        )
                );
    }

    @Override
    public Mono<Set<String>> findBlockedSeatIds(
            UUID eventId
    ) {

        return mongoTemplate
                .findById(
                        eventId,
                        EventAvailabilityDocument.class
                )
                .map(document -> {

                    if (document.blockedSeatIds() == null) {
                        return Set.<String>of();
                    }

                    return copyOf(
                            document.blockedSeatIds()
                    );
                })
                .defaultIfEmpty(of());
    }
}
