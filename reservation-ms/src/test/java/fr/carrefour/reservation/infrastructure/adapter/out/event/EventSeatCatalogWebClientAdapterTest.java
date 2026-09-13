package fr.carrefour.reservation.infrastructure.adapter.out.event;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class EventSeatCatalogWebClientAdapterTest {

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "11111111-1111-1111-1111-111111111111"
            );

    @Test
    void shouldGetSeatCandidatesFromEventService() {

        AtomicReference<ClientRequest> capturedRequest =
                new AtomicReference<>();

        WebClient webClient =
                WebClient.builder()
                        .baseUrl(
                                "http://event-ms:8080"
                        )
                        .exchangeFunction(request -> {

                            capturedRequest.set(
                                    request
                            );

                            return Mono.just(
                                    ClientResponse
                                            .create(
                                                    HttpStatus.OK
                                            )
                                            .header(
                                                    "Content-Type",
                                                    MediaType.APPLICATION_JSON_VALUE
                                            )
                                            .body(
                                                    """
                                                    {
                                                      "eventId":
                                                        "11111111-1111-1111-1111-111111111111",
                                                      "seatIds": [
                                                        "A-01-01",
                                                        "A-01-02",
                                                        "A-01-03"
                                                      ]
                                                    }
                                                    """
                                            )
                                            .build()
                            );
                        })
                        .build();

        EventSeatCatalogWebClientAdapter adapter =
                new EventSeatCatalogWebClientAdapter(
                        webClient
                );

        StepVerifier
                .create(
                        adapter.getSeatCandidates(
                                EVENT_ID,
                                20
                        )
                )
                .assertNext(result -> {

                    assertThat(
                            result.eventId()
                    ).isEqualTo(
                            EVENT_ID
                    );

                    assertThat(
                            result.seatIds()
                    ).containsExactly(
                            "A-01-01",
                            "A-01-02",
                            "A-01-03"
                    );
                })
                .verifyComplete();

        ClientRequest request =
                capturedRequest.get();

        assertThat(
                request.method().name()
        ).isEqualTo(
                "GET"
        );

        assertThat(
                request.url().getPath()
        ).isEqualTo(
                "/internal/events/"
                        + EVENT_ID
                        + "/seat-candidates"
        );

        assertThat(
                request.url()
                        .getQuery()
        ).isEqualTo(
                "limit=20"
        );
    }

    @Test
    void shouldReturnEmptySeatListWhenEventHasNoCandidates() {

        WebClient webClient =
                WebClient.builder()
                        .baseUrl(
                                "http://event-ms:8080"
                        )
                        .exchangeFunction(request ->
                                Mono.just(
                                        ClientResponse
                                                .create(
                                                        HttpStatus.OK
                                                )
                                                .header(
                                                        "Content-Type",
                                                        MediaType.APPLICATION_JSON_VALUE
                                                )
                                                .body(
                                                        """
                                                        {
                                                          "eventId":
                                                            "11111111-1111-1111-1111-111111111111",
                                                          "seatIds": []
                                                        }
                                                        """
                                                )
                                                .build()
                                )
                        )
                        .build();

        EventSeatCatalogWebClientAdapter adapter =
                new EventSeatCatalogWebClientAdapter(
                        webClient
                );

        StepVerifier
                .create(
                        adapter.getSeatCandidates(
                                EVENT_ID,
                                20
                        )
                )
                .assertNext(result -> {

                    assertThat(
                            result.eventId()
                    ).isEqualTo(
                            EVENT_ID
                    );

                    assertThat(
                            result.seatIds()
                    ).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void shouldPropagateEventServiceNotFoundResponse() {

        WebClient webClient =
                WebClient.builder()
                        .baseUrl(
                                "http://event-ms:8080"
                        )
                        .exchangeFunction(request ->
                                Mono.just(
                                        ClientResponse
                                                .create(
                                                        HttpStatus.NOT_FOUND
                                                )
                                                .header(
                                                        "Content-Type",
                                                        MediaType.APPLICATION_JSON_VALUE
                                                )
                                                .body(
                                                        """
                                                        {
                                                          "message":
                                                            "Event not found"
                                                        }
                                                        """
                                                )
                                                .build()
                                )
                        )
                        .build();

        EventSeatCatalogWebClientAdapter adapter =
                new EventSeatCatalogWebClientAdapter(
                        webClient
                );

        StepVerifier
                .create(
                        adapter.getSeatCandidates(
                                EVENT_ID,
                                20
                        )
                )
                .expectError(
                        WebClientResponseException.NotFound.class
                )
                .verify();
    }

    @Test
    void shouldPropagateEventServiceFailure() {

        WebClient webClient =
                WebClient.builder()
                        .baseUrl(
                                "http://event-ms:8080"
                        )
                        .exchangeFunction(request ->
                                Mono.just(
                                        ClientResponse
                                                .create(
                                                        HttpStatus.INTERNAL_SERVER_ERROR
                                                )
                                                .body(
                                                        "Event service unavailable"
                                                )
                                                .build()
                                )
                        )
                        .build();

        EventSeatCatalogWebClientAdapter adapter =
                new EventSeatCatalogWebClientAdapter(
                        webClient
                );

        StepVerifier
                .create(
                        adapter.getSeatCandidates(
                                EVENT_ID,
                                20
                        )
                )
                .expectError(
                        WebClientResponseException
                                .InternalServerError.class
                )
                .verify();
    }
}