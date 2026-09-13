package fr.carrefour.event.infrastructure.adapter.out.persistence.mongodb;

import fr.carrefour.event.application.port.out.EventAvailabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest(
        properties = {
                "spring.mongodb.representation.uuid=standard"
        }
)
@Import(MongoEventAvailabilityRepositoryAdapter.class)
@Testcontainers
class MongoEventAvailabilityRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo =
            new MongoDBContainer("mongo:8.0");

    @Autowired
    private EventAvailabilityRepository repository;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    @BeforeEach
    void cleanDatabase() {

        StepVerifier.create(
                        mongoTemplate
                                .remove(
                                        new Query(),
                                        "event_availability"
                                )
                )
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldInitializeAvailability() {

        UUID eventId =
                UUID.randomUUID();

        StepVerifier.create(
                        repository
                                .initialize(
                                        eventId,
                                        10
                                )
                                .then(
                                        repository.findByEventId(
                                                eventId
                                        )
                                )
                )
                .assertNext(availability -> {

                    assertThat(
                            availability.eventId()
                    )
                            .isEqualTo(eventId);

                    assertThat(
                            availability.totalSeats()
                    )
                            .isEqualTo(10);

                    assertThat(
                            availability.remainingSeats()
                    )
                            .isEqualTo(10);
                })
                .verifyComplete();
    }

    @Test
    void shouldBlockSeatAndDecreaseRemainingSeats() {

        UUID eventId =
                UUID.randomUUID();

        StepVerifier.create(
                        repository
                                .initialize(
                                        eventId,
                                        10
                                )
                                .then(
                                        repository.blockSeat(
                                                eventId,
                                                "A-01-01"
                                        )
                                )
                                .then(
                                        repository.findByEventId(
                                                eventId
                                        )
                                )
                )
                .assertNext(availability -> {

                    assertThat(
                            availability.totalSeats()
                    )
                            .isEqualTo(10);

                    assertThat(
                            availability.remainingSeats()
                    )
                            .isEqualTo(9);
                })
                .verifyComplete();

        StepVerifier.create(
                        repository.findBlockedSeatIds(
                                eventId
                        )
                )
                .assertNext(blockedSeatIds ->
                        assertThat(blockedSeatIds)
                                .containsExactly(
                                        "A-01-01"
                                )
                )
                .verifyComplete();
    }

    @Test
    void shouldNotBlockSameSeatTwice() {

        UUID eventId =
                UUID.randomUUID();

        StepVerifier.create(
                        repository
                                .initialize(
                                        eventId,
                                        10
                                )
                                .then(
                                        repository.blockSeat(
                                                eventId,
                                                "A-01-01"
                                        )
                                )
                                .then(
                                        repository.blockSeat(
                                                eventId,
                                                "A-01-01"
                                        )
                                )
                                .then(
                                        repository.findByEventId(
                                                eventId
                                        )
                                )
                )
                .assertNext(availability ->
                        assertThat(
                                availability.remainingSeats()
                        )
                                .isEqualTo(9)
                )
                .verifyComplete();

        StepVerifier.create(
                        repository.findBlockedSeatIds(
                                eventId
                        )
                )
                .assertNext(blockedSeatIds ->
                        assertThat(blockedSeatIds)
                                .containsExactly(
                                        "A-01-01"
                                )
                )
                .verifyComplete();
    }

    @Test
    void shouldReleaseBlockedSeatAndIncreaseRemainingSeats() {

        UUID eventId =
                UUID.randomUUID();

        StepVerifier.create(
                        repository
                                .initialize(
                                        eventId,
                                        10
                                )
                                .then(
                                        repository.blockSeat(
                                                eventId,
                                                "A-01-01"
                                        )
                                )
                                .then(
                                        repository.releaseSeat(
                                                eventId,
                                                "A-01-01"
                                        )
                                )
                                .then(
                                        repository.findByEventId(
                                                eventId
                                        )
                                )
                )
                .assertNext(availability ->
                        assertThat(
                                availability.remainingSeats()
                        )
                                .isEqualTo(10)
                )
                .verifyComplete();

        StepVerifier.create(
                        repository.findBlockedSeatIds(
                                eventId
                        )
                )
                .assertNext(blockedSeatIds ->
                        assertThat(blockedSeatIds)
                                .isEmpty()
                )
                .verifyComplete();
    }

    @Test
    void shouldNotReleaseSameSeatTwice() {

        UUID eventId =
                UUID.randomUUID();

        StepVerifier.create(
                        repository
                                .initialize(
                                        eventId,
                                        10
                                )
                                .then(
                                        repository.blockSeat(
                                                eventId,
                                                "A-01-01"
                                        )
                                )
                                .then(
                                        repository.releaseSeat(
                                                eventId,
                                                "A-01-01"
                                        )
                                )
                                .then(
                                        repository.releaseSeat(
                                                eventId,
                                                "A-01-01"
                                        )
                                )
                                .then(
                                        repository.findByEventId(
                                                eventId
                                        )
                                )
                )
                .assertNext(availability ->
                        assertThat(
                                availability.remainingSeats()
                        )
                                .isEqualTo(10)
                )
                .verifyComplete();

        StepVerifier.create(
                        repository.findBlockedSeatIds(
                                eventId
                        )
                )
                .assertNext(blockedSeatIds ->
                        assertThat(blockedSeatIds)
                                .isEmpty()
                )
                .verifyComplete();
    }

    @Test
    void shouldNotResetAvailabilityWhenInitializedAgain() {

        UUID eventId =
                UUID.randomUUID();

        StepVerifier.create(
                        repository
                                .initialize(
                                        eventId,
                                        10
                                )
                                .then(
                                        repository.blockSeat(
                                                eventId,
                                                "A-01-01"
                                        )
                                )
                                .then(
                                        repository.initialize(
                                                eventId,
                                                10
                                        )
                                )
                                .then(
                                        repository.findByEventId(
                                                eventId
                                        )
                                )
                )
                .assertNext(availability -> {

                    assertThat(
                            availability.totalSeats()
                    )
                            .isEqualTo(10);

                    assertThat(
                            availability.remainingSeats()
                    )
                            .isEqualTo(9);
                })
                .verifyComplete();

        StepVerifier.create(
                        repository.findBlockedSeatIds(
                                eventId
                        )
                )
                .assertNext(blockedSeatIds ->
                        assertThat(blockedSeatIds)
                                .containsExactly(
                                        "A-01-01"
                                )
                )
                .verifyComplete();
    }

    @Test
    void shouldReturnBlockedSeatIds() {

        UUID eventId =
                UUID.randomUUID();

        StepVerifier.create(
                        repository
                                .initialize(
                                        eventId,
                                        10
                                )
                                .then(
                                        repository.blockSeat(
                                                eventId,
                                                "A-01-01"
                                        )
                                )
                                .then(
                                        repository.blockSeat(
                                                eventId,
                                                "A-01-02"
                                        )
                                )
                                .then(
                                        repository.findBlockedSeatIds(
                                                eventId
                                        )
                                )
                )
                .assertNext(blockedSeatIds ->
                        assertThat(blockedSeatIds)
                                .containsExactlyInAnyOrder(
                                        "A-01-01",
                                        "A-01-02"
                                )
                )
                .verifyComplete();
    }

    @Test
    void shouldNeverDecreaseRemainingSeatsBelowZero() {

        UUID eventId =
                UUID.randomUUID();

        StepVerifier.create(
                        repository
                                .initialize(
                                        eventId,
                                        1
                                )
                                .then(
                                        repository.blockSeat(
                                                eventId,
                                                "A-01-01"
                                        )
                                )
                                .then(
                                        repository.blockSeat(
                                                eventId,
                                                "A-01-02"
                                        )
                                )
                                .then(
                                        repository.findByEventId(
                                                eventId
                                        )
                                )
                )
                .assertNext(availability -> {

                    assertThat(
                            availability.totalSeats()
                    )
                            .isEqualTo(1);

                    assertThat(
                            availability.remainingSeats()
                    )
                            .isZero();
                })
                .verifyComplete();

        StepVerifier.create(
                        repository.findBlockedSeatIds(
                                eventId
                        )
                )
                .assertNext(blockedSeatIds ->
                        assertThat(blockedSeatIds)
                                .containsExactly(
                                        "A-01-01"
                                )
                )
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenAvailabilityDoesNotExist() {

        UUID eventId =
                UUID.randomUUID();

        StepVerifier.create(
                        repository.findByEventId(
                                eventId
                        )
                )
                .verifyComplete();
    }
}