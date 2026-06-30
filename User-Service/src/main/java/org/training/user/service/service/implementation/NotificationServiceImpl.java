package org.training.user.service.service.implementation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.training.user.service.exception.ResourceNotFound;
import org.training.user.service.model.dto.notification.NotificationRequest;
import org.training.user.service.model.dto.notification.NotificationResponse;
import org.training.user.service.model.entity.Notification;
import org.training.user.service.repository.NotificationRepository;
import org.training.user.service.repository.UserRepository;
import org.training.user.service.service.NotificationService;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    public List<NotificationResponse> getNotifications(Long userId) {
        ensureUserExists(userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public NotificationResponse createNotification(NotificationRequest request) {
        ensureUserExists(request.getUserId());
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .title(normalize(request.getTitle()))
                .message(normalize(request.getMessage()))
                .type(normalize(request.getType()))
                .referenceId(normalizeOptional(request.getReferenceId()))
                .read(false)
                .build();
        return toResponse(notificationRepository.save(notification));
    }

    @Override
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        ensureUserExists(userId);
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFound("Không tìm thấy thông báo"));
        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    private void ensureUserExists(Long userId) {
        if (userId == null || userRepository.findById(userId).isEmpty()) {
            throw new ResourceNotFound("Không tìm thấy khách hàng");
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .referenceId(notification.getReferenceId())
                .read(notification.getRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOptional(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? null : normalized;
    }
}
