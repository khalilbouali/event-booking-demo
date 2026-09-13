package fr.carrefour.event.application.service;

import fr.carrefour.event.application.model.EventSummary;
import fr.carrefour.event.application.port.out.EventAvailabilityRepository;
import fr.carrefour.event.application.port.out.EventRepository;
import fr.carrefour.event.domain.exception.EventNotFoundException;
import fr.carrefour.event.domain.model.Event;
import fr.carrefour.event.domain.model.EventAvailability;
import fr.carrefour.event.domain.model.Seat;
import fr.carrefour.event.domain.service.SeatLayoutGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventApplicationServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventAvailabilityRepository availabilityRepository;

    @Mock
    private SeatLayoutGenerator seatLayoutGenerator;

    @Mock
    private EventAvailability availability;

    private EventApplicationService service;

    @BeforeEach
    void setUp() {
        service = new EventApplicationService(
                eventRepository,
                availabilityRepository,
                seatLayoutGenerator
        );
    }

    @Test
    void shouldCreateEventAndInitializeAvailability() {

        List<Seat> seats = List.of(
                seat("A-01-01"),
                seat("A-01-02")
        );

        when(
                seatLayoutGenerator.generate(2)
        ).thenReturn(seats);

        when(
                eventRepository.save(any(Event.class))
        ).thenAnswer(invocation ->
                Mono.just(
                        invocation.getArgument(0)
                )
        );

        when(
                availabilityRepository.initialize(
                        any(UUID.class),
                        eq(2)
                )
        ).thenReturn(Mono.empty());

        when(
                availabilityRepository.findByEventId(
                        any(UUID.class)
                )
        ).thenReturn(
                Mono.just(availability)
        );

        when(
                availability.remainingSeats()
        ).thenReturn(2);

        StepVerifier.create(
                        service.createEvent(
                                "Carrefour Tech Event",
                                "Casablanca",
                                Instant.parse(
                                        "2026-10-01T18:00:00Z"
                                ),
                                2,
                                new BigDecimal("150.00")
                        )
                )
                .assertNext(summary -> {

                    assertThat(summary.id())
                            .isNotNull();

                    assertThat(summary.name())
                            .isEqualTo(
                                    "Carrefour Tech Event"
                            );

                    assertThat(summary.venue())
                            .isEqualTo(
                                    "Casablanca"
                            );

                    assertThat(summary.startsAt())
                            .isEqualTo(
                                    Instant.parse(
                                            "2026-10-01T18:00:00Z"
                                    )
                            );

                    assertThat(summary.seatCount())
                            .isEqualTo(2);

                    assertThat(
                            summary.remainingSeatCount()
                    )
                            .isEqualTo(2);

                    assertThat(summary.price())
                            .isEqualByComparingTo(
                                    "150.00"
                            );
                })
                .verifyComplete();

        verify(
                seatLayoutGenerator
        ).generate(2);

        ArgumentCaptor<Event> eventCaptor =
                ArgumentCaptor.forClass(
                        Event.class
                );

        verify(eventRepository)
                .save(eventCaptor.capture());

        Event savedEvent =
                eventCaptor.getValue();

        assertThat(savedEvent.getSeats())
                .hasSize(2);

        assertThat(savedEvent.getPrice())
                .isEqualByComparingTo(
                        "150.00"
                );

        verify(availabilityRepository)
                .initialize(
                        savedEvent.getId(),
                        2
                );

        verify(availabilityRepository)
                .findByEventId(
                        savedEvent.getId()
                );
    }

    @Test
    void shouldGetEventById() {

        UUID eventId =
                UUID.randomUUID();

        Event event =
                event(
                        eventId,
                        "Java Conference",
                        3
                );

        when(
                eventRepository.findById(eventId)
        ).thenReturn(
                Mono.just(event)
        );

        when(
                availabilityRepository
                        .findByEventId(eventId)
        ).thenReturn(
                Mono.just(availability)
        );

        when(
                availability.remainingSeats()
        ).thenReturn(2);

        StepVerifier.create(
                        service.getById(eventId)
                )
                .assertNext(summary -> {

                    assertThat(summary.id())
                            .isEqualTo(eventId);

                    assertThat(summary.name())
                            .isEqualTo(
                                    "Java Conference"
                            );

                    assertThat(summary.seatCount())
                            .isEqualTo(3);

                    assertThat(
                            summary.remainingSeatCount()
                    )
                            .isEqualTo(2);
                })
                .verifyComplete();

        verify(eventRepository)
                .findById(eventId);

        verify(availabilityRepository)
                .findByEventId(eventId);

        verify(
                availabilityRepository,
                never()
        ).initialize(
                any(UUID.class),
                anyInt()
        );
    }

    @Test
    void shouldFailWhenEventDoesNotExist() {

        UUID eventId =
                UUID.randomUUID();

        when(
                eventRepository.findById(eventId)
        ).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        service.getById(eventId)
                )
                .expectError(
                        EventNotFoundException.class
                )
                .verify();

        verify(eventRepository)
                .findById(eventId);

        verifyNoInteractions(
                availabilityRepository
        );
    }

    @Test
    void shouldListAllEventsWithAvailability() {

        UUID firstId =
                UUID.randomUUID();

        UUID secondId =
                UUID.randomUUID();

        Event first =
                event(
                        firstId,
                        "Event One",
                        10
                );

        Event second =
                event(
                        secondId,
                        "Event Two",
                        20
                );

        EventAvailability firstAvailability =
                mock(EventAvailability.class);

        EventAvailability secondAvailability =
                mock(EventAvailability.class);

        when(
                eventRepository.findAll()
        ).thenReturn(
                Flux.just(
                        first,
                        second
                )
        );

        when(
                availabilityRepository
                        .findByEventId(firstId)
        ).thenReturn(
                Mono.just(firstAvailability)
        );

        when(
                availabilityRepository
                        .findByEventId(secondId)
        ).thenReturn(
                Mono.just(secondAvailability)
        );

        when(
                firstAvailability.remainingSeats()
        ).thenReturn(7);

        when(
                secondAvailability.remainingSeats()
        ).thenReturn(15);

        StepVerifier.create(
                        service
                                .findAll()
                                .collectMap(
                                        EventSummary::id
                                )
                )
                .assertNext(events -> {

                    assertThat(events)
                            .hasSize(2);

                    EventSummary firstSummary =
                            events.get(firstId);

                    assertThat(
                            firstSummary.name()
                    )
                            .isEqualTo(
                                    "Event One"
                            );

                    assertThat(
                            firstSummary.seatCount()
                    )
                            .isEqualTo(10);

                    assertThat(
                            firstSummary
                                    .remainingSeatCount()
                    )
                            .isEqualTo(7);

                    EventSummary secondSummary =
                            events.get(secondId);

                    assertThat(
                            secondSummary.name()
                    )
                            .isEqualTo(
                                    "Event Two"
                            );

                    assertThat(
                            secondSummary.seatCount()
                    )
                            .isEqualTo(20);

                    assertThat(
                            secondSummary
                                    .remainingSeatCount()
                    )
                            .isEqualTo(15);
                })
                .verifyComplete();

        verify(eventRepository)
                .findAll();

        verify(availabilityRepository)
                .findByEventId(firstId);

        verify(availabilityRepository)
                .findByEventId(secondId);
    }

    @Test
    void shouldGetEventDetailsByIds() {

        UUID firstId =
                UUID.randomUUID();

        UUID secondId =
                UUID.randomUUID();

        Event first =
                event(
                        firstId,
                        "Event One",
                        5
                );

        Event second =
                event(
                        secondId,
                        "Event Two",
                        5
                );

        List<UUID> ids =
                List.of(
                        firstId,
                        secondId
                );

        when(
                eventRepository.findAllById(ids)
        ).thenReturn(
                Flux.just(
                        first,
                        second
                )
        );

        StepVerifier.create(
                        service.getByIds(ids)
                )
                .assertNext(details -> {

                    assertThat(details.id())
                            .isEqualTo(firstId);

                    assertThat(details.name())
                            .isEqualTo(
                                    "Event One"
                            );

                    assertThat(details.venue())
                            .isEqualTo(
                                    "Casablanca"
                            );

                    assertThat(details.startsAt())
                            .isEqualTo(
                                    Instant.parse(
                                            "2026-10-01T18:00:00Z"
                                    )
                            );
                })
                .assertNext(details ->
                        assertThat(details.id())
                                .isEqualTo(secondId)
                )
                .verifyComplete();

        verify(eventRepository)
                .findAllById(ids);

        verifyNoInteractions(
                availabilityRepository
        );
    }

    @Test
    void shouldPropagateRepositoryErrorWhenCreatingEvent() {

        RuntimeException failure =
                new RuntimeException(
                        "Mongo unavailable"
                );

        when(
                seatLayoutGenerator.generate(1)
        ).thenReturn(
                List.of(
                        seat("A-01-01")
                )
        );

        when(
                eventRepository.save(
                        any(Event.class)
                )
        ).thenReturn(
                Mono.error(failure)
        );

        StepVerifier.create(
                        service.createEvent(
                                "Event",
                                "Rabat",
                                Instant.parse(
                                        "2026-10-01T18:00:00Z"
                                ),
                                1,
                                BigDecimal.TEN
                        )
                )
                .expectErrorSatisfies(error ->
                        assertThat(error)
                                .isSameAs(failure)
                )
                .verify();

        verifyNoInteractions(
                availabilityRepository
        );
    }

    @Test
    void shouldStopCreationWhenAvailabilityInitializationFails() {

        RuntimeException failure =
                new RuntimeException(
                        "Availability initialization failed"
                );

        when(
                seatLayoutGenerator.generate(1)
        ).thenReturn(
                List.of(
                        seat("A-01-01")
                )
        );

        when(
                eventRepository.save(
                        any(Event.class)
                )
        ).thenAnswer(invocation ->
                Mono.just(
                        invocation.getArgument(0)
                )
        );

        when(
                availabilityRepository.initialize(
                        any(UUID.class),
                        eq(1)
                )
        ).thenReturn(
                Mono.error(failure)
        );

        StepVerifier.create(
                        service.createEvent(
                                "Event",
                                "Rabat",
                                Instant.parse(
                                        "2026-10-01T18:00:00Z"
                                ),
                                1,
                                BigDecimal.TEN
                        )
                )
                .expectErrorSatisfies(error ->
                        assertThat(error)
                                .isSameAs(failure)
                )
                .verify();

        verify(
                availabilityRepository,
                never()
        ).findByEventId(
                any(UUID.class)
        );
    }

    @Test
    void shouldInitializeMissingAvailabilityWhenGettingEvent() {

        UUID eventId =
                UUID.randomUUID();

        Event event =
                event(
                        eventId,
                        "Java Conference",
                        10
                );

        EventAvailability initializedAvailability =
                mock(EventAvailability.class);

        when(
                eventRepository.findById(eventId)
        ).thenReturn(
                Mono.just(event)
        );

        when(
                availabilityRepository.findByEventId(eventId)
        )
                .thenReturn(Mono.empty())
                .thenReturn(
                        Mono.just(
                                initializedAvailability
                        )
                );

        when(
                availabilityRepository.initialize(
                        eventId,
                        10
                )
        ).thenReturn(
                Mono.empty()
        );

        when(
                initializedAvailability.remainingSeats()
        ).thenReturn(10);

        StepVerifier.create(
                        service.getById(eventId)
                )
                .assertNext(summary -> {

                    assertThat(summary.id())
                            .isEqualTo(eventId);

                    assertThat(summary.seatCount())
                            .isEqualTo(10);

                    assertThat(
                            summary.remainingSeatCount()
                    )
                            .isEqualTo(10);
                })
                .verifyComplete();

        verify(
                availabilityRepository,
                times(2)
        ).findByEventId(eventId);

        verify(
                availabilityRepository
        ).initialize(
                eventId,
                10
        );
    }

    @Test
    void shouldFailIfAvailabilityIsStillMissingAfterInitialization() {

        UUID eventId =
                UUID.randomUUID();

        Event event =
                event(
                        eventId,
                        "Java Conference",
                        10
                );

        when(
                eventRepository.findById(eventId)
        ).thenReturn(
                Mono.just(event)
        );

        when(
                availabilityRepository.findByEventId(eventId)
        ).thenReturn(
                Mono.empty()
        );

        when(
                availabilityRepository.initialize(
                        eventId,
                        10
                )
        ).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        service.getById(eventId)
                )
                .expectErrorSatisfies(error -> {

                    assertThat(error)
                            .isInstanceOf(
                                    IllegalStateException.class
                            );

                    assertThat(error.getMessage())
                            .contains(
                                    "Availability projection could not be initialized"
                            );
                })
                .verify();

        verify(
                availabilityRepository
        ).initialize(
                eventId,
                10
        );
    }

    private static Event event(
            UUID id,
            String name,
            int seatCount
    ) {

        List<Seat> seats =
                new SeatLayoutGenerator()
                        .generate(seatCount);

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

    private static Seat seat(
            String id
    ) {

        String[] parts =
                id.split("-");

        return new Seat(
                id,
                parts[0],
                parts[1],
                parts[2]
        );
    }
}