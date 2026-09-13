package fr.carrefour.notification.application.service;

import fr.carrefour.notification.application.port.in.SendNotificationUseCase;
import fr.carrefour.notification.application.port.out.NotificationSender;
import fr.carrefour.notification.domain.model.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public final class NotificationApplicationService
        implements SendNotificationUseCase {

    private final NotificationSender notificationSender;

    @Override
    public void send(
            String customerId,
            String seatId,
            NotificationType type
    ) {
        notificationSender.send(
                customerId,
                seatId,
                type
        );
    }
}
