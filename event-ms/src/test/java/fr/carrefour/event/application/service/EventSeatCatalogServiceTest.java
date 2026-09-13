package fr.carrefour.event.application.service;

import fr.carrefour.event.application.port.out.EventAvailabilityRepository;
import fr.carrefour.event.application.port.out.EventRepository;
import fr.carrefour.event.domain.exception.EventNotFoundException;
import fr.carrefour.event.domain.model.Event;
import fr.carrefour.event.domain.model.Seat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventSeatCatalogServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventAvailabilityRepository
            availabilityRepository;

    @InjectMocks
    private EventSeatCatalogService service;

    @Test
    void shouldReturnAvailableSeatCandidates() {

        UUID eventId = UUID.randomUUID();

        Event event = mock(Event.class);

        when(event.getSeats())
                .thenReturn(
                        List.of(
                                seat("A-01-01"),
                                seat("A-01-02"),
                                seat("A-01-03"),
                                seat("A-01-04")
                        )
                );

        when(
                eventRepository.findById(eventId)
        ).thenReturn(
                Mono.just(event)
        );

        when(
                availabilityRepository
                        .findBlockedSeatIds(eventId)
        ).thenReturn(
                Mono.just(
                        Set.of(
                                "A-01-02",
                                "A-01-04"
                        )
                )
        );

        StepVerifier
                .create(
                        service.getSeatCandidates(
                                eventId,
                                10
                        )
                )
                .assertNext(result -> {

                    assertThat(
                            result.eventId()
                    ).isEqualTo(
                            eventId
                    );

                    assertThat(
                            result.seatIds()
                    ).containsExactly(
                            "A-01-01",
                            "A-01-03"
                    );
                })
                .verifyComplete();

        verify(
                eventRepository
        ).findById(
                eventId
        );

        verify(
                availabilityRepository
        ).findBlockedSeatIds(
                eventId
        );
    }

    @Test
    void shouldRespectRequestedLimit() {

        UUID eventId = UUID.randomUUID();

        Event event = mock(Event.class);

        when(event.getSeats())
                .thenReturn(
                        List.of(
                                seat("A-01-01"),
                                seat("A-01-02"),
                                seat("A-01-03"),
                                seat("A-01-04")
                        )
                );

        when(
                eventRepository.findById(eventId)
        ).thenReturn(
                Mono.just(event)
        );

        when(
                availabilityRepository
                        .findBlockedSeatIds(eventId)
        ).thenReturn(
                Mono.just(
                        Set.of()
                )
        );

        StepVerifier
                .create(
                        service.getSeatCandidates(
                                eventId,
                                2
                        )
                )
                .assertNext(result ->
                        assertThat(
                                result.seatIds()
                        ).containsExactly(
                                "A-01-01",
                                "A-01-02"
                        )
                )
                .verifyComplete();
    }

    @Test
    void shouldApplyLimitAfterRemovingBlockedSeats() {

        UUID eventId = UUID.randomUUID();

        Event event = mock(Event.class);

        when(event.getSeats())
                .thenReturn(
                        List.of(
                                seat("A-01-01"),
                                seat("A-01-02"),
                                seat("A-01-03"),
                                seat("A-01-04"),
                                seat("A-01-05")
                        )
                );

        when(
                eventRepository.findById(eventId)
        ).thenReturn(
                Mono.just(event)
        );

        when(
                availabilityRepository
                        .findBlockedSeatIds(eventId)
        ).thenReturn(
                Mono.just(
                        Set.of(
                                "A-01-01",
                                "A-01-02"
                        )
                )
        );

        StepVerifier
                .create(
                        service.getSeatCandidates(
                                eventId,
                                2
                        )
                )
                .assertNext(result ->
                        assertThat(
                                result.seatIds()
                        ).containsExactly(
                                "A-01-03",
                                "A-01-04"
                        )
                )
                .verifyComplete();
    }

    @Test
    void shouldPreserveSeatCatalogOrder() {

        UUID eventId = UUID.randomUUID();

        Event event = mock(Event.class);

        when(event.getSeats())
                .thenReturn(
                        List.of(
                                seat("A-01-03"),
                                seat("A-01-01"),
                                seat("A-01-04"),
                                seat("A-01-02")
                        )
                );

        when(
                eventRepository.findById(eventId)
        ).thenReturn(
                Mono.just(event)
        );

        when(
                availabilityRepository
                        .findBlockedSeatIds(eventId)
        ).thenReturn(
                Mono.just(
                        Set.of(
                                "A-01-04"
                        )
                )
        );

        StepVerifier
                .create(
                        service.getSeatCandidates(
                                eventId,
                                10
                        )
                )
                .assertNext(result ->
                        assertThat(
                                result.seatIds()
                        ).containsExactly(
                                "A-01-03",
                                "A-01-01",
                                "A-01-02"
                        )
                )
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyCandidateListWhenAllSeatsAreBlocked() {

        UUID eventId = UUID.randomUUID();

        Event event = mock(Event.class);

        when(event.getSeats())
                .thenReturn(
                        List.of(
                                seat("A-01-01"),
                                seat("A-01-02")
                        )
                );

        when(
                eventRepository.findById(eventId)
        ).thenReturn(
                Mono.just(event)
        );

        when(
                availabilityRepository
                        .findBlockedSeatIds(eventId)
        ).thenReturn(
                Mono.just(
                        Set.of(
                                "A-01-01",
                                "A-01-02"
                        )
                )
        );

        StepVerifier
                .create(
                        service.getSeatCandidates(
                                eventId,
                                10
                        )
                )
                .assertNext(result -> {

                    assertThat(
                            result.eventId()
                    ).isEqualTo(
                            eventId
                    );

                    assertThat(
                            result.seatIds()
                    ).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void shouldThrowEventNotFoundWhenEventDoesNotExist() {

        UUID eventId = UUID.randomUUID();

        when(
                eventRepository.findById(eventId)
        ).thenReturn(
                Mono.empty()
        );

        StepVerifier
                .create(
                        service.getSeatCandidates(
                                eventId,
                                10
                        )
                )
                .expectError(
                        EventNotFoundException.class
                )
                .verify();

        verify(
                eventRepository
        ).findById(
                eventId
        );

        verifyNoInteractions(
                availabilityRepository
        );
    }

    @Test
    void shouldPropagateAvailabilityRepositoryFailure() {

        UUID eventId = UUID.randomUUID();

        Event event = mock(Event.class);

        RuntimeException failure =
                new RuntimeException(
                        "Mongo unavailable"
                );

        when(
                eventRepository.findById(eventId)
        ).thenReturn(
                Mono.just(event)
        );

        when(
                availabilityRepository
                        .findBlockedSeatIds(eventId)
        ).thenReturn(
                Mono.error(failure)
        );

        StepVerifier
                .create(
                        service.getSeatCandidates(
                                eventId,
                                10
                        )
                )
                .expectErrorMatches(
                        exception ->
                                exception == failure
                )
                .verify();

        verify(
                availabilityRepository
        ).findBlockedSeatIds(
                eventId
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