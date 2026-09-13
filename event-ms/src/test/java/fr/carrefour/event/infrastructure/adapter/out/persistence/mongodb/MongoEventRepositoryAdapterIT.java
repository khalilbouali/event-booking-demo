package fr.carrefour.event.infrastructure.adapter.out.persistence.mongodb;

import fr.carrefour.event.application.port.out.EventRepository;
import fr.carrefour.event.domain.model.Event;
import fr.carrefour.event.domain.model.Seat;
import fr.carrefour.event.infrastructure.adapter.out.persistence.mongodb.repository.ReactiveMongoEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest(
        properties = {
                "spring.mongodb.representation.uuid=standard"
        }
)
@Import({
        MongoEventRepositoryAdapter.class,
        EventPersistenceMapper.class
})
@Testcontainers
class MongoEventRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo =
            new MongoDBContainer(
                    "mongo:8.0"
            );

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ReactiveMongoEventRepository mongoRepository;

    @BeforeEach
    void cleanDatabase() {

        StepVerifier.create(
                        mongoRepository.deleteAll()
                )
                .verifyComplete();
    }

    @Test
    void shouldSaveAndFindEventById() {

        Event event = event(
                UUID.randomUUID(),
                "Carrefour Tech Event"
        );

        StepVerifier.create(
                        eventRepository
                                .save(event)
                                .flatMap(saved ->
                                        eventRepository
                                                .findById(
                                                        saved.getId()
                                                )
                                )
                )
                .assertNext(found -> {

                    assertThat(found.getId())
                            .isEqualTo(event.getId());

                    assertThat(found.getName())
                            .isEqualTo(
                                    "Carrefour Tech Event"
                            );

                    assertThat(found.getVenue())
                            .isEqualTo(
                                    "Casablanca"
                            );

                    assertThat(found.getStartsAt())
                            .isEqualTo(
                                    Instant.parse(
                                            "2026-10-01T18:00:00Z"
                                    )
                            );

                    assertThat(found.getPrice())
                            .isEqualByComparingTo(
                                    "150.00"
                            );

                    assertThat(found.getSeats())
                            .hasSize(2);

                    assertThat(found.getSeats())
                            .extracting(Seat::id)
                            .containsExactly(
                                    "A-01-01",
                                    "A-01-02"
                            );
                })
                .verifyComplete();
    }

    @Test
    void shouldFindAllEvents() {

        Event first =
                event(
                        UUID.randomUUID(),
                        "Event One"
                );

        Event second =
                event(
                        UUID.randomUUID(),
                        "Event Two"
                );

        StepVerifier.create(
                        eventRepository
                                .save(first)
                                .then(
                                        eventRepository.save(
                                                second
                                        )
                                )
                                .thenMany(
                                        eventRepository.findAll()
                                )
                                .collectList()
                )
                .assertNext(events -> {

                    assertThat(events)
                            .hasSize(2);

                    assertThat(events)
                            .extracting(Event::getId)
                            .containsExactlyInAnyOrder(
                                    first.getId(),
                                    second.getId()
                            );
                })
                .verifyComplete();
    }

    @Test
    void shouldFindEventsByIds() {

        Event first =
                event(
                        UUID.randomUUID(),
                        "Event One"
                );

        Event second =
                event(
                        UUID.randomUUID(),
                        "Event Two"
                );

        Event third =
                event(
                        UUID.randomUUID(),
                        "Event Three"
                );

        StepVerifier.create(
                        eventRepository
                                .save(first)
                                .then(
                                        eventRepository.save(
                                                second
                                        )
                                )
                                .then(
                                        eventRepository.save(
                                                third
                                        )
                                )
                                .thenMany(
                                        eventRepository.findAllById(
                                                List.of(
                                                        first.getId(),
                                                        third.getId()
                                                )
                                        )
                                )
                                .collectList()
                )
                .assertNext(events ->

                    assertThat(events)
                            .hasSize(2)
                            .extracting(Event::getId)
                            .containsExactlyInAnyOrder(
                                    first.getId(),
                                    third.getId()
                            )
                )
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenEventDoesNotExist() {

        UUID unknownId =
                UUID.randomUUID();

        StepVerifier.create(
                        eventRepository.findById(
                                unknownId
                        )
                )
                .verifyComplete();
    }

    private static Event event(
            UUID id,
            String name
    ) {

        List<Seat> seats = List.of(
                new Seat(
                        "A-01-01",
                        "A",
                        "01",
                        "01"
                ),
                new Seat(
                        "A-01-02",
                        "A",
                        "01",
                        "02"
                )
        );

        return Event.restore(
                id,
                name,
                "Casablanca",
                Instant.parse(
                        "2026-10-01T18:00:00Z"
                ),
                seats,
                new BigDecimal("150.00")
        );
    }
}
