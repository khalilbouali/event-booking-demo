package fr.carrefour.reservation.domain.model;

import fr.carrefour.reservation.domain.exception.InvalidReservationStateException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static fr.carrefour.reservation.domain.model.ReservationStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    private static final UUID EVENT_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID RESERVATION_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final String SEAT_ID =
            "A-01-01";

    private static final String CUSTOMER_ID =
            "customer-123";

    private static final Instant NOW =
            Instant.parse("2026-09-10T10:00:00Z");

    private static final Duration HOLD_DURATION =
            Duration.ofMinutes(10);

    @Test
    void shouldHoldReservation() {

        Reservation reservation =
                Reservation.hold(
                        EVENT_ID,
                        SEAT_ID,
                        CUSTOMER_ID,
                        NOW,
                        HOLD_DURATION
                );

        assertThat(reservation.getId())
                .isNotNull();

        assertThat(reservation.getEventId())
                .isEqualTo(EVENT_ID);

        assertThat(reservation.getSeatId())
                .isEqualTo(SEAT_ID);

        assertThat(reservation.getCustomerId())
                .isEqualTo(CUSTOMER_ID);

        assertThat(reservation.getStatus())
                .isEqualTo(HELD);

        assertThat(reservation.getCreatedAt())
                .isEqualTo(NOW);

        assertThat(reservation.getExpiresAt())
                .isEqualTo(
                        NOW.plus(HOLD_DURATION)
                );

        assertThat(
                reservation.blocksSeatAvailability()
        ).isTrue();
    }

    @Test
    void shouldRestoreReservation() {

        Instant expiresAt =
                NOW.plus(HOLD_DURATION);

        Reservation reservation =
                Reservation.restore(
                        RESERVATION_ID,
                        EVENT_ID,
                        SEAT_ID,
                        CUSTOMER_ID,
                        CONFIRMED,
                        NOW,
                        expiresAt
                );

        assertThat(reservation.getId())
                .isEqualTo(RESERVATION_ID);

        assertThat(reservation.getEventId())
                .isEqualTo(EVENT_ID);

        assertThat(reservation.getSeatId())
                .isEqualTo(SEAT_ID);

        assertThat(reservation.getCustomerId())
                .isEqualTo(CUSTOMER_ID);

        assertThat(reservation.getStatus())
                .isEqualTo(CONFIRMED);

        assertThat(reservation.getCreatedAt())
                .isEqualTo(NOW);

        assertThat(reservation.getExpiresAt())
                .isEqualTo(expiresAt);
    }

    @Test
    void shouldConfirmHeldReservationBeforeExpiration() {

        Reservation reservation =
                heldReservation();

        reservation.confirm(
                NOW.plusSeconds(30)
        );

        assertThat(reservation.getStatus())
                .isEqualTo(CONFIRMED);

        assertThat(
                reservation.blocksSeatAvailability()
        ).isTrue();
    }

    @Test
    void shouldKeepConfirmedReservationConfirmedWhenConfirmCalledAgain() {

        Reservation reservation =
                restoredReservation(CONFIRMED);

        reservation.confirm(
                NOW.plus(Duration.ofHours(1))
        );

        assertThat(reservation.getStatus())
                .isEqualTo(CONFIRMED);
    }

    @Test
    void shouldRejectConfirmationAtExpirationTime() {

        Reservation reservation =
                heldReservation();

        assertThatThrownBy(
                () ->
                        reservation.confirm(
                                reservation.getExpiresAt()
                        )
        )
                .isInstanceOf(
                        InvalidReservationStateException.class
                )
                .hasMessage(
                        "Expired reservation cannot be confirmed"
                );

        assertThat(reservation.getStatus())
                .isEqualTo(HELD);
    }

    @Test
    void shouldRejectConfirmationAfterExpiration() {

        Reservation reservation =
                heldReservation();

        assertThatThrownBy(
                () ->
                        reservation.confirm(
                                reservation
                                        .getExpiresAt()
                                        .plusSeconds(1)
                        )
        )
                .isInstanceOf(
                        InvalidReservationStateException.class
                )
                .hasMessage(
                        "Expired reservation cannot be confirmed"
                );

        assertThat(reservation.getStatus())
                .isEqualTo(HELD);
    }

    @Test
    void shouldRejectConfirmationOfCancelledReservation() {

        Reservation reservation =
                restoredReservation(CANCELLED);

        assertThatThrownBy(
                () -> reservation.confirm(NOW)
        )
                .isInstanceOf(
                        InvalidReservationStateException.class
                )
                .hasMessage(
                        "Operation is only allowed for held reservations"
                );
    }

    @Test
    void shouldRejectConfirmationOfExpiredReservation() {

        Reservation reservation =
                restoredReservation(EXPIRED);

        assertThatThrownBy(
                () -> reservation.confirm(NOW)
        )
                .isInstanceOf(
                        InvalidReservationStateException.class
                )
                .hasMessage(
                        "Operation is only allowed for held reservations"
                );
    }

    @Test
    void shouldExpireHeldReservationAtExpirationTime() {

        Reservation reservation =
                heldReservation();

        reservation.expire(
                reservation.getExpiresAt()
        );

        assertThat(reservation.getStatus())
                .isEqualTo(EXPIRED);

        assertThat(
                reservation.blocksSeatAvailability()
        ).isFalse();
    }

    @Test
    void shouldExpireHeldReservationAfterExpirationTime() {

        Reservation reservation =
                heldReservation();

        reservation.expire(
                reservation
                        .getExpiresAt()
                        .plusSeconds(1)
        );

        assertThat(reservation.getStatus())
                .isEqualTo(EXPIRED);
    }

    @Test
    void shouldNotExpireHeldReservationBeforeExpirationTime() {

        Reservation reservation =
                heldReservation();

        reservation.expire(
                reservation
                        .getExpiresAt()
                        .minusSeconds(1)
        );

        assertThat(reservation.getStatus())
                .isEqualTo(HELD);
    }

    @Test
    void shouldNotExpireConfirmedReservation() {

        Reservation reservation =
                restoredReservation(CONFIRMED);

        reservation.expire(
                NOW.plus(Duration.ofHours(1))
        );

        assertThat(reservation.getStatus())
                .isEqualTo(CONFIRMED);
    }

    @Test
    void shouldCancelHeldReservation() {

        Reservation reservation =
                heldReservation();

        reservation.cancel();

        assertThat(reservation.getStatus())
                .isEqualTo(CANCELLED);

        assertThat(
                reservation.blocksSeatAvailability()
        ).isFalse();
    }

    @Test
    void shouldKeepCancelledReservationCancelledWhenCancelCalledAgain() {

        Reservation reservation =
                restoredReservation(CANCELLED);

        reservation.cancel();

        assertThat(reservation.getStatus())
                .isEqualTo(CANCELLED);
    }

    @Test
    void shouldRejectCancellationOfConfirmedReservation() {

        Reservation reservation =
                restoredReservation(CONFIRMED);

        assertThatThrownBy(
                reservation::cancel
        )
                .isInstanceOf(
                        InvalidReservationStateException.class
                )
                .hasMessage(
                        "Operation is only allowed for held reservations"
                );
    }

    @Test
    void shouldRejectCancellationOfExpiredReservation() {

        Reservation reservation =
                restoredReservation(EXPIRED);

        assertThatThrownBy(
                reservation::cancel
        )
                .isInstanceOf(
                        InvalidReservationStateException.class
                )
                .hasMessage(
                        "Operation is only allowed for held reservations"
                );
    }

    @Test
    void shouldConsiderHeldReservationExpiredAtExpirationTime() {

        Reservation reservation =
                heldReservation();

        assertThat(
                reservation.isExpiredAt(
                        reservation.getExpiresAt()
                )
        ).isTrue();
    }

    @Test
    void shouldNotConsiderHeldReservationExpiredBeforeExpirationTime() {

        Reservation reservation =
                heldReservation();

        assertThat(
                reservation.isExpiredAt(
                        reservation
                                .getExpiresAt()
                                .minusSeconds(1)
                )
        ).isFalse();
    }

    @Test
    void shouldNotConsiderConfirmedReservationExpired() {

        Reservation reservation =
                restoredReservation(CONFIRMED);

        assertThat(
                reservation.isExpiredAt(
                        NOW.plus(Duration.ofHours(1))
                )
        ).isFalse();
    }

    @Test
    void shouldReportSeatBlockingAccordingToReservationStatus() {

        assertThat(
                restoredReservation(HELD)
                        .blocksSeatAvailability()
        ).isTrue();

        assertThat(
                restoredReservation(CONFIRMED)
                        .blocksSeatAvailability()
        ).isTrue();

        assertThat(
                restoredReservation(EXPIRED)
                        .blocksSeatAvailability()
        ).isFalse();

        assertThat(
                restoredReservation(CANCELLED)
                        .blocksSeatAvailability()
        ).isFalse();
    }

    @Test
    void shouldRejectNullEventId() {

        assertThatThrownBy(
                () ->
                        Reservation.hold(
                                null,
                                SEAT_ID,
                                CUSTOMER_ID,
                                NOW,
                                HOLD_DURATION
                        )
        )
                .isInstanceOf(
                        NullPointerException.class
                )
                .hasMessage(
                        "Event id is required"
                );
    }

    @Test
    void shouldRejectNullSeatId() {

        assertThatThrownBy(
                () ->
                        Reservation.hold(
                                EVENT_ID,
                                null,
                                CUSTOMER_ID,
                                NOW,
                                HOLD_DURATION
                        )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Seat id is required"
                );
    }

    @Test
    void shouldRejectBlankSeatId() {

        assertThatThrownBy(
                () ->
                        Reservation.hold(
                                EVENT_ID,
                                "   ",
                                CUSTOMER_ID,
                                NOW,
                                HOLD_DURATION
                        )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Seat id is required"
                );
    }

    @Test
    void shouldRejectBlankCustomerId() {

        assertThatThrownBy(
                () ->
                        Reservation.hold(
                                EVENT_ID,
                                SEAT_ID,
                                " ",
                                NOW,
                                HOLD_DURATION
                        )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Customer id is required"
                );
    }

    @Test
    void shouldRejectNullCurrentTimeWhenHolding() {

        assertThatThrownBy(
                () ->
                        Reservation.hold(
                                EVENT_ID,
                                SEAT_ID,
                                CUSTOMER_ID,
                                null,
                                HOLD_DURATION
                        )
        )
                .isInstanceOf(
                        NullPointerException.class
                )
                .hasMessage(
                        "Current time is required"
                );
    }

    @Test
    void shouldRejectNullHoldDuration() {

        assertThatThrownBy(
                () ->
                        Reservation.hold(
                                EVENT_ID,
                                SEAT_ID,
                                CUSTOMER_ID,
                                NOW,
                                null
                        )
        )
                .isInstanceOf(
                        NullPointerException.class
                )
                .hasMessage(
                        "Hold duration is required"
                );
    }

    @Test
    void shouldRejectZeroHoldDuration() {

        assertThatThrownBy(
                () ->
                        Reservation.hold(
                                EVENT_ID,
                                SEAT_ID,
                                CUSTOMER_ID,
                                NOW,
                                Duration.ZERO
                        )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Hold duration must be greater than zero"
                );
    }

    @Test
    void shouldRejectNegativeHoldDuration() {

        assertThatThrownBy(
                () ->
                        Reservation.hold(
                                EVENT_ID,
                                SEAT_ID,
                                CUSTOMER_ID,
                                NOW,
                                Duration.ofSeconds(-1)
                        )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Hold duration must be greater than zero"
                );
    }

    @Test
    void shouldRejectExpirationBeforeCreationWhenRestoring() {

        assertThatThrownBy(
                () ->
                        Reservation.restore(
                                RESERVATION_ID,
                                EVENT_ID,
                                SEAT_ID,
                                CUSTOMER_ID,
                                HELD,
                                NOW,
                                NOW.minusSeconds(1)
                        )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Expiration date cannot be before creation date"
                );
    }

    @Test
    void shouldRejectNullTimeWhenConfirming() {

        Reservation reservation =
                heldReservation();

        assertThatThrownBy(
                () -> reservation.confirm(null)
        )
                .isInstanceOf(
                        NullPointerException.class
                )
                .hasMessage(
                        "Current time is required"
                );
    }

    @Test
    void shouldRejectNullTimeWhenExpiring() {

        Reservation reservation =
                heldReservation();

        assertThatThrownBy(
                () -> reservation.expire(null)
        )
                .isInstanceOf(
                        NullPointerException.class
                )
                .hasMessage(
                        "Current time is required"
                );
    }

    @Test
    void shouldRejectNullTimeWhenCheckingExpiration() {

        Reservation reservation =
                heldReservation();

        assertThatThrownBy(
                () -> reservation.isExpiredAt(null)
        )
                .isInstanceOf(
                        NullPointerException.class
                )
                .hasMessage(
                        "Current time is required"
                );
    }

    private Reservation heldReservation() {

        return Reservation.hold(
                EVENT_ID,
                SEAT_ID,
                CUSTOMER_ID,
                NOW,
                HOLD_DURATION
        );
    }

    private Reservation restoredReservation(
            ReservationStatus status
    ) {

        return Reservation.restore(
                RESERVATION_ID,
                EVENT_ID,
                SEAT_ID,
                CUSTOMER_ID,
                status,
                NOW,
                NOW.plus(HOLD_DURATION)
        );
    }
}