package fr.carrefour.event.infrastructure.adapter.in.web;

import fr.carrefour.event.application.model.EventDetails;
import fr.carrefour.event.application.model.EventSeatCandidates;
import fr.carrefour.event.application.port.in.GetEventSeatCandidatesUseCase;
import fr.carrefour.event.application.port.in.GetEventsByIdsUseCase;
import fr.carrefour.event.infrastructure.configuration.security.SecurityConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@WebFluxTest(
        controllers = InternalEventController.class,
        properties = {
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/mock-jwks",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/mock-issuer"
        }
)
@Import(SecurityConfiguration.class)
class InternalEventControllerTest {

    @Autowired
    private ApplicationContext applicationContext;

    private WebTestClient webTestClient;

    @MockitoBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @MockitoBean
    private GetEventSeatCandidatesUseCase getEventSeatCandidatesUseCase;

    @MockitoBean
    private GetEventsByIdsUseCase getEventsByIdsUseCase;

    @BeforeEach
    void setUp() {

        webTestClient = WebTestClient
                .bindToApplicationContext(applicationContext)
                .apply(springSecurity())
                .configureClient()
                .build();
    }

    @Test
    void shouldRejectUnauthenticatedRequest() {

        UUID eventId =
                UUID.randomUUID();

        webTestClient
                .get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path(
                                        "/internal/events/details"
                                )
                                .queryParam(
                                        "ids",
                                        eventId
                                )
                                .build()
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(
                getEventsByIdsUseCase,
                never()
        ).getByIds(
                anyCollection()
        );
    }

    @Test
    void shouldReturnEventDetailsForAuthenticatedRequest() {

        UUID firstId =
                UUID.randomUUID();

        UUID secondId =
                UUID.randomUUID();

        EventDetails first =
                new EventDetails(
                        firstId,
                        "Java Conference",
                        "Casablanca",
                        Instant.parse(
                                "2026-10-01T18:00:00Z"
                        )
                );

        EventDetails second =
                new EventDetails(
                        secondId,
                        "Spring Conference",
                        "Rabat",
                        Instant.parse(
                                "2026-11-01T10:00:00Z"
                        )
                );

        when(
                getEventsByIdsUseCase.getByIds(
                        anyCollection()
                )
        ).thenReturn(
                Flux.just(
                        first,
                        second
                )
        );

        webTestClient
                .mutateWith(
                        mockJwt()
                )
                .get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path(
                                        "/internal/events/details"
                                )
                                .queryParam(
                                        "ids",
                                        firstId,
                                        secondId
                                )
                                .build()
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[0].id")
                .isEqualTo(
                        firstId.toString()
                )
                .jsonPath("$[0].name")
                .isEqualTo(
                        "Java Conference"
                )
                .jsonPath("$[0].venue")
                .isEqualTo(
                        "Casablanca"
                )
                .jsonPath("$[0].startsAt")
                .isEqualTo(
                        "2026-10-01T18:00:00Z"
                )
                .jsonPath("$[1].id")
                .isEqualTo(
                        secondId.toString()
                )
                .jsonPath("$[1].name")
                .isEqualTo(
                        "Spring Conference"
                )
                .jsonPath("$[1].venue")
                .isEqualTo(
                        "Rabat"
                )
                .jsonPath("$[1].startsAt")
                .isEqualTo(
                        "2026-11-01T10:00:00Z"
                );

        verify(
                getEventsByIdsUseCase
        ).getByIds(
                org.mockito.ArgumentMatchers.argThat(
                        ids -> {
                            assertThat(ids)
                                    .containsExactlyInAnyOrder(
                                            firstId,
                                            secondId
                                    );

                            return true;
                        }
                )
        );
    }

    @Test
    void shouldReturnEmptyArrayWhenNoEventsAreFound() {

        UUID eventId =
                UUID.randomUUID();

        when(
                getEventsByIdsUseCase.getByIds(
                        anyCollection()
                )
        ).thenReturn(
                Flux.empty()
        );

        webTestClient
                .mutateWith(
                        mockJwt()
                )
                .get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path(
                                        "/internal/events/details"
                                )
                                .queryParam(
                                        "ids",
                                        eventId
                                )
                                .build()
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json("[]");
    }

    @Test
    void shouldReturnSeatCandidatesForAuthenticatedRequest() {

        UUID eventId =
                UUID.randomUUID();

        EventSeatCandidates candidates =
                mock(EventSeatCandidates.class);

        when(
                candidates.eventId()
        ).thenReturn(
                eventId
        );

        when(
                candidates.seatIds()
        ).thenReturn(
                List.of(
                        "A-01-01",
                        "A-01-02",
                        "A-01-03"
                )
        );

        when(
                getEventSeatCandidatesUseCase
                        .getSeatCandidates(
                                eventId,
                                3
                        )
        ).thenReturn(
                Mono.just(candidates)
        );

        webTestClient
                .mutateWith(
                        mockJwt()
                )
                .get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path(
                                        "/internal/events/{eventId}/seat-candidates"
                                )
                                .queryParam(
                                        "limit",
                                        3
                                )
                                .build(eventId)
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.eventId")
                .isEqualTo(
                        eventId.toString()
                )
                .jsonPath("$.seatIds.length()")
                .isEqualTo(3)
                .jsonPath("$.seatIds[0]")
                .isEqualTo(
                        "A-01-01"
                )
                .jsonPath("$.seatIds[1]")
                .isEqualTo(
                        "A-01-02"
                )
                .jsonPath("$.seatIds[2]")
                .isEqualTo(
                        "A-01-03"
                );

        verify(
                getEventSeatCandidatesUseCase
        ).getSeatCandidates(
                eventId,
                3
        );
    }

    @Test
    void shouldUseDefaultLimitOfTwentyWhenLimitIsNotProvided() {

        UUID eventId =
                UUID.randomUUID();

        EventSeatCandidates candidates =
                mock(EventSeatCandidates.class);

        when(
                candidates.eventId()
        ).thenReturn(
                eventId
        );

        when(
                candidates.seatIds()
        ).thenReturn(
                List.of(
                        "A-01-01",
                        "A-01-02"
                )
        );

        when(
                getEventSeatCandidatesUseCase
                        .getSeatCandidates(
                                eventId,
                                20
                        )
        ).thenReturn(
                Mono.just(candidates)
        );

        webTestClient
                .mutateWith(
                        mockJwt()
                )
                .get()
                .uri(
                        "/internal/events/{eventId}/seat-candidates",
                        eventId
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.eventId")
                .isEqualTo(
                        eventId.toString()
                )
                .jsonPath("$.seatIds.length()")
                .isEqualTo(2);

        verify(
                getEventSeatCandidatesUseCase
        ).getSeatCandidates(
                eventId,
                20
        );
    }

    @Test
    void shouldReturnEmptySeatCandidates() {

        UUID eventId =
                UUID.randomUUID();

        EventSeatCandidates candidates =
                mock(EventSeatCandidates.class);

        when(
                candidates.eventId()
        ).thenReturn(
                eventId
        );

        when(
                candidates.seatIds()
        ).thenReturn(
                List.of()
        );

        when(
                getEventSeatCandidatesUseCase
                        .getSeatCandidates(
                                eventId,
                                10
                        )
        ).thenReturn(
                Mono.just(candidates)
        );

        webTestClient
                .mutateWith(
                        mockJwt()
                )
                .get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path(
                                        "/internal/events/{eventId}/seat-candidates"
                                )
                                .queryParam(
                                        "limit",
                                        10
                                )
                                .build(eventId)
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.eventId")
                .isEqualTo(
                        eventId.toString()
                )
                .jsonPath("$.seatIds")
                .isArray()
                .jsonPath("$.seatIds.length()")
                .isEqualTo(0);

        verify(
                getEventSeatCandidatesUseCase
        ).getSeatCandidates(
                eventId,
                10
        );
    }

    @Test
    void shouldRejectUnauthenticatedSeatCandidatesRequest() {

        UUID eventId =
                UUID.randomUUID();

        webTestClient
                .get()
                .uri(
                        "/internal/events/{eventId}/seat-candidates",
                        eventId
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(
                getEventSeatCandidatesUseCase,
                never()
        ).getSeatCandidates(
                any(UUID.class),
                anyInt()
        );
    }

    @Test
    void shouldRejectLimitBelowMinimum() {

        UUID eventId =
                UUID.randomUUID();

        webTestClient
                .mutateWith(
                        mockJwt()
                )
                .get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path(
                                        "/internal/events/{eventId}/seat-candidates"
                                )
                                .queryParam(
                                        "limit",
                                        0
                                )
                                .build(eventId)
                )
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo(400)
                .jsonPath("$.message")
                .isEqualTo(
                        "must be greater than or equal to 1"
                );

        verify(
                getEventSeatCandidatesUseCase,
                never()
        ).getSeatCandidates(
                any(UUID.class),
                anyInt()
        );
    }

    @Test
    void shouldRejectLimitAboveMaximum() {

        UUID eventId =
                UUID.randomUUID();

        webTestClient
                .mutateWith(
                        mockJwt()
                )
                .get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path(
                                        "/internal/events/{eventId}/seat-candidates"
                                )
                                .queryParam(
                                        "limit",
                                        101
                                )
                                .build(eventId)
                )
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo(400)
                .jsonPath("$.message")
                .isEqualTo(
                        "must be less than or equal to 100"
                );

        verify(
                getEventSeatCandidatesUseCase,
                never()
        ).getSeatCandidates(
                any(UUID.class),
                anyInt()
        );
    }
}