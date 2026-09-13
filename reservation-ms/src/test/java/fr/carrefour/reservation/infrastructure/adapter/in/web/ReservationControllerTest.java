package fr.carrefour.reservation.infrastructure.adapter.in.web;

import fr.carrefour.reservation.application.port.in.GetReservationUseCase;
import fr.carrefour.reservation.application.port.in.HoldSeatUseCase;
import fr.carrefour.reservation.application.port.in.ListMyReservationsUseCase;
import fr.carrefour.reservation.domain.model.Reservation;
import fr.carrefour.reservation.infrastructure.adapter.in.web.dto.ReservationResponse;
import fr.carrefour.reservation.infrastructure.configuration.security.SecurityConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static fr.carrefour.reservation.domain.model.ReservationStatus.CONFIRMED;
import static fr.carrefour.reservation.domain.model.ReservationStatus.HELD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@WebFluxTest(
        controllers = ReservationController.class,
        properties = {
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/mock-jwks",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/mock-issuer"
        }
)
@Import(SecurityConfiguration.class)
class ReservationControllerTest {

    private static final String CUSTOMER_ID =
            "customer-123";

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "11111111-1111-1111-1111-111111111111"
            );

    private static final UUID RESERVATION_ID =
            UUID.fromString(
                    "22222222-2222-2222-2222-222222222222"
            );

    private static final Instant NOW =
            Instant.parse(
                    "2026-09-10T10:00:00Z"
            );

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private HoldSeatUseCase holdSeatUseCase;

    @MockitoBean
    private GetReservationUseCase getReservationUseCase;

    @MockitoBean
    private ListMyReservationsUseCase listMyReservationsUseCase;

    @MockitoBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {

        webTestClient =
                WebTestClient
                        .bindToApplicationContext(
                                applicationContext
                        )
                        .apply(
                                springSecurity()
                        )
                        .configureClient()
                        .build();
    }

    @Test
    void shouldRejectUnauthenticatedHoldRequest() {

        webTestClient
                .post()
                .uri(
                        "/api/reservations"
                )
                .bodyValue(
                        """
                        {
                          "eventId":
                            "11111111-1111-1111-1111-111111111111"
                        }
                        """
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verifyNoInteractions(
                holdSeatUseCase
        );
    }

    @Test
    void shouldHoldSeatForAuthenticatedCustomer() {

        Reservation reservation =
                heldReservation(
                        CUSTOMER_ID
                );

        when(
                holdSeatUseCase.hold(
                        EVENT_ID,
                        CUSTOMER_ID
                )
        ).thenReturn(
                Mono.just(
                        reservation
                )
        );

        webTestClient
                .mutateWith(
                        mockJwt()
                                .jwt(jwt ->
                                        jwt.subject(
                                                CUSTOMER_ID
                                        )
                                )
                )
                .post()
                .uri(
                        "/api/reservations"
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        """
                        {
                          "eventId":
                            "11111111-1111-1111-1111-111111111111"
                        }
                        """
                )
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(
                        ReservationResponse.class
                )
                .value(response ->
                        assertThat(response)
                                .usingRecursiveComparison()
                                .isEqualTo(
                                        ReservationResponse.from(
                                                reservation
                                        )
                                )
                );

        verify(
                holdSeatUseCase
        ).hold(
                EVENT_ID,
                CUSTOMER_ID
        );
    }

    @Test
    void shouldUseJwtSubjectAsCustomerIdWhenHolding() {

        String jwtSubject =
                "jwt-customer-456";

        Reservation reservation =
                heldReservation(
                        jwtSubject
                );

        when(
                holdSeatUseCase.hold(
                        EVENT_ID,
                        jwtSubject
                )
        ).thenReturn(
                Mono.just(
                        reservation
                )
        );

        webTestClient
                .mutateWith(
                        mockJwt()
                                .jwt(jwt ->
                                        jwt.subject(
                                                jwtSubject
                                        )
                                )
                )
                .post()
                .uri(
                        "/api/reservations"
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        """
                        {
                          "eventId":
                            "11111111-1111-1111-1111-111111111111"
                        }
                        """
                )
                .exchange()
                .expectStatus()
                .isCreated();

        verify(
                holdSeatUseCase
        ).hold(
                EVENT_ID,
                jwtSubject
        );
    }

    @Test
    void shouldGetReservationById() {

        Reservation reservation =
                heldReservation(
                        CUSTOMER_ID
                );

        when(
                getReservationUseCase.getById(
                        RESERVATION_ID
                )
        ).thenReturn(
                Mono.just(
                        reservation
                )
        );

        webTestClient
                .mutateWith(
                        mockJwt()
                                .jwt(jwt ->
                                        jwt.subject(
                                                CUSTOMER_ID
                                        )
                                )
                )
                .get()
                .uri(
                        "/api/reservations/{id}",
                        RESERVATION_ID
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(
                        ReservationResponse.class
                )
                .value(response ->
                        assertThat(response)
                                .usingRecursiveComparison()
                                .isEqualTo(
                                        ReservationResponse.from(
                                                reservation
                                        )
                                )
                );

        verify(
                getReservationUseCase
        ).getById(
                RESERVATION_ID
        );
    }

    @Test
    void shouldRejectUnauthenticatedGetById() {

        webTestClient
                .get()
                .uri(
                        "/api/reservations/{id}",
                        RESERVATION_ID
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verifyNoInteractions(
                getReservationUseCase
        );
    }

    @Test
    void shouldFindReservationsForAuthenticatedCustomer() {

        Reservation first =
                heldReservation(
                        CUSTOMER_ID
                );

        Reservation second =
                confirmedReservation(
                        UUID.randomUUID()
                );

        when(
                listMyReservationsUseCase
                        .findByCustomerId(
                                CUSTOMER_ID
                        )
        ).thenReturn(
                Flux.just(
                        first,
                        second
                )
        );

        List<ReservationResponse> expected =
                List.of(
                        ReservationResponse.from(
                                first
                        ),
                        ReservationResponse.from(
                                second
                        )
                );

        webTestClient
                .mutateWith(
                        mockJwt()
                                .jwt(jwt ->
                                        jwt.subject(
                                                CUSTOMER_ID
                                        )
                                )
                )
                .get()
                .uri(
                        "/api/reservations"
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(
                        ReservationResponse.class
                )
                .value(responses ->
                        assertThat(responses)
                                .usingRecursiveComparison()
                                .isEqualTo(
                                        expected
                                )
                );

        verify(
                listMyReservationsUseCase
        ).findByCustomerId(
                CUSTOMER_ID
        );
    }

    @Test
    void shouldReturnEmptyListWhenCustomerHasNoReservations() {

        when(
                listMyReservationsUseCase
                        .findByCustomerId(
                                CUSTOMER_ID
                        )
        ).thenReturn(
                Flux.empty()
        );

        webTestClient
                .mutateWith(
                        mockJwt()
                                .jwt(jwt ->
                                        jwt.subject(
                                                CUSTOMER_ID
                                        )
                                )
                )
                .get()
                .uri(
                        "/api/reservations"
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(
                        "[]"
                );

        verify(
                listMyReservationsUseCase
        ).findByCustomerId(
                CUSTOMER_ID
        );
    }

    @Test
    void shouldRejectUnauthenticatedFindMineRequest() {

        webTestClient
                .get()
                .uri(
                        "/api/reservations"
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verifyNoInteractions(
                listMyReservationsUseCase
        );
    }

    @Test
    void shouldRejectInvalidEventId() {

        webTestClient
                .mutateWith(
                        mockJwt()
                                .jwt(jwt ->
                                        jwt.subject(
                                                CUSTOMER_ID
                                        )
                                )
                )
                .post()
                .uri(
                        "/api/reservations"
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        """
                        {
                          "eventId": "not-a-uuid"
                        }
                        """
                )
                .exchange()
                .expectStatus()
                .isBadRequest();

        verifyNoInteractions(
                holdSeatUseCase
        );
    }

    private Reservation heldReservation(
            String customerId
    ) {

        return Reservation.restore(
                ReservationControllerTest.RESERVATION_ID,
                ReservationControllerTest.EVENT_ID,
                "A-01-01",
                customerId,
                HELD,
                NOW,
                NOW.plus(
                        Duration.ofMinutes(10)
                )
        );
    }

    private Reservation confirmedReservation(
            UUID reservationId
    ) {

        return Reservation.restore(
                reservationId,
                ReservationControllerTest.EVENT_ID,
                "A-01-02",
                ReservationControllerTest.CUSTOMER_ID,
                CONFIRMED,
                NOW,
                NOW.plus(
                        Duration.ofMinutes(10)
                )
        );
    }
}