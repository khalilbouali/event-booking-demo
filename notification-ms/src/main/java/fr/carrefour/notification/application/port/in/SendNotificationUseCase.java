package fr.carrefour.notification.application.port.in;

import fr.carrefour.notification.domain.model.NotificationType;

public interface SendNotificationUseCase {

    void send(
            String customerId,
            String seatId,
            NotificationType type
    );
}
