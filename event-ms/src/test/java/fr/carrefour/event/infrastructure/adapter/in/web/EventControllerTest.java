package fr.carrefour.event.infrastructure.adapter.in.web;

import fr.carrefour.event.application.model.EventSummary;
import fr.carrefour.event.application.port.in.CreateEventUseCase;
import fr.carrefour.event.application.port.in.GetEventUseCase;
import fr.carrefour.event.application.port.in.ListEventsUseCase;
import fr.carrefour.event.infrastructure.configuration.security.SecurityConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@WebFluxTest(
        controllers = EventController.class,
        properties = {
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/mock-jwks",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/mock-issuer"
        }
)
@Import(SecurityConfiguration.class)
class EventControllerTest {

    @Autowired
    private ApplicationContext applicationContext;

    private WebTestClient webTestClient;

    @MockitoBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @MockitoBean
    private CreateEventUseCase createEventUseCase;

    @MockitoBean
    private GetEventUseCase getEventUseCase;

    @MockitoBean
    private ListEventsUseCase listEventsUseCase;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient
                .bindToApplicationContext(applicationContext)
                .apply(springSecurity())
                .configureClient()
                .build();
    }

    @Test
    void shouldRejectUnauthenticatedGetEvents() {

        webTestClient
                .get()
                .uri("/api/events")
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(
                listEventsUseCase,
                never()
        ).findAll();
    }

    @Test
    void shouldAllowAuthenticatedUserToListEvents() {

        UUID eventId =
                UUID.randomUUID();

        EventSummary summary =
                eventSummary(eventId);

        when(
                listEventsUseCase.findAll()
        ).thenReturn(
                Flux.just(summary)
        );

        webTestClient
                .mutateWith(
                        mockJwt()
                                .authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_user"
                                        )
                                )
                )
                .get()
                .uri("/api/events")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[0].id")
                .isEqualTo(
                        eventId.toString()
                )
                .jsonPath("$[0].name")
                .isEqualTo(
                        "Carrefour Tech Event"
                )
                .jsonPath("$[0].venue")
                .isEqualTo(
                        "Casablanca"
                )
                .jsonPath("$[0].seatCount")
                .isEqualTo(10)
                .jsonPath(
                        "$[0].remainingSeatCount"
                )
                .isEqualTo(7)
                .jsonPath("$[0].price")
                .isEqualTo(150.00);

        verify(listEventsUseCase)
                .findAll();
    }

    @Test
    void shouldAllowAuthenticatedUserToGetEventById() {

        UUID eventId =
                UUID.randomUUID();

        when(
                getEventUseCase.getById(eventId)
        ).thenReturn(
                Mono.just(
                        eventSummary(eventId)
                )
        );

        webTestClient
                .mutateWith(
                        mockJwt()
                                .authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_user"
                                        )
                                )
                )
                .get()
                .uri(
                        "/api/events/{eventId}",
                        eventId
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo(
                        eventId.toString()
                )
                .jsonPath("$.name")
                .isEqualTo(
                        "Carrefour Tech Event"
                );

        verify(getEventUseCase)
                .getById(eventId);
    }

    @Test
    void shouldRejectNormalUserWhenCreatingEvent() {

        webTestClient
                .mutateWith(
                        mockJwt()
                                .authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_user"
                                        )
                                )
                )
                .post()
                .uri("/api/events")
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(
                        """
                        {
                          "name": "Carrefour Tech Event",
                          "venue": "Casablanca",
                          "startsAt": "2026-10-01T18:00:00Z",
                          "seatCount": 10,
                          "price": 150.00
                        }
                        """
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(
                createEventUseCase,
                never()
        ).createEvent(
                anyString(),
                anyString(),
                any(Instant.class),
                anyInt(),
                any(BigDecimal.class)
        );
    }

    @Test
    void shouldAllowAdministratorToCreateEvent() {

        UUID eventId =
                UUID.randomUUID();

        EventSummary summary =
                eventSummary(eventId);

        when(
                createEventUseCase.createEvent(
                        eq("Carrefour Tech Event"),
                        eq("Casablanca"),
                        eq(
                                Instant.parse(
                                        "2026-10-01T18:00:00Z"
                                )
                        ),
                        eq(10),
                        eq(
                                new BigDecimal(
                                        "150.00"
                                )
                        )
                )
        ).thenReturn(
                Mono.just(summary)
        );

        webTestClient
                .mutateWith(
                        mockJwt()
                                .authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_administrator"
                                        )
                                )
                )
                .post()
                .uri("/api/events")
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(
                        """
                        {
                          "name": "Carrefour Tech Event",
                          "venue": "Casablanca",
                          "startsAt": "2026-10-01T18:00:00Z",
                          "seatCount": 10,
                          "price": 150.00
                        }
                        """
                )
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo(
                        eventId.toString()
                )
                .jsonPath("$.name")
                .isEqualTo(
                        "Carrefour Tech Event"
                )
                .jsonPath("$.venue")
                .isEqualTo(
                        "Casablanca"
                )
                .jsonPath("$.seatCount")
                .isEqualTo(10)
                .jsonPath(
                        "$.remainingSeatCount"
                )
                .isEqualTo(7)
                .jsonPath("$.price")
                .isEqualTo(150.00);

        verify(createEventUseCase)
                .createEvent(
                        eq("Carrefour Tech Event"),
                        eq("Casablanca"),
                        eq(
                                Instant.parse(
                                        "2026-10-01T18:00:00Z"
                                )
                        ),
                        eq(10),
                        eq(
                                new BigDecimal(
                                        "150.00"
                                )
                        )
                );
    }

    private static EventSummary eventSummary(
            UUID eventId
    ) {

        return new EventSummary(
                eventId,
                "Carrefour Tech Event",
                "Casablanca",
                Instant.parse(
                        "2026-10-01T18:00:00Z"
                ),
                10,
                7,
                new BigDecimal("150.00")
        );
    }
}