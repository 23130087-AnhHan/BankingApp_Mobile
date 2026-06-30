package org.training.user.service.service;

import org.training.user.service.model.dto.notification.NotificationRequest;
import org.training.user.service.model.dto.notification.NotificationResponse;

import java.util.List;

public interface NotificationService {

    List<NotificationResponse> getNotifications(Long userId);

    NotificationResponse createNotification(NotificationRequest request);

    NotificationResponse markAsRead(Long userId, Long notificationId);
}
