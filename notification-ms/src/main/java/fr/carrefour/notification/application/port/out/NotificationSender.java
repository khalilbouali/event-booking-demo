package fr.carrefour.notification.application.port.out;

import fr.carrefour.notification.domain.model.NotificationType;

public interface NotificationSender {

    void send(
            String customerId,
            String seatId,
            NotificationType type
    );
}