package fr.carrefour.notification.infrastructure.adapter.out.notification;

import fr.carrefour.notification.application.port.out.NotificationSender;
import fr.carrefour.notification.domain.model.NotificationType;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This LoggingNotificationSender serves as an example,
 * the idea is implementing an EmailNotificationSender using spring mail or SmsNotificationSender later on
 */
@Component
public final class LoggingNotificationSender
        implements NotificationSender {

    private static final Logger LOGGER =
            getLogger(LoggingNotificationSender.class);

    @Override
    public void send(
            String customerId,
            String seatId,
            NotificationType type
    ) {
        LOGGER.info(
                "Sending notification: customerId={}, seatId={}, type={}",
                customerId,
                seatId,
                type
        );
    }
}