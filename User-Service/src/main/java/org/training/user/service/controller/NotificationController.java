package org.training.user.service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.training.user.service.model.dto.notification.NotificationRequest;
import org.training.user.service.model.dto.notification.NotificationResponse;
import org.training.user.service.service.NotificationService;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/{userId}/notifications")
    public ResponseEntity<List<NotificationResponse>> getNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getNotifications(userId));
    }

    @PostMapping("/notifications")
    public ResponseEntity<NotificationResponse> createNotification(@Valid @RequestBody NotificationRequest request) {
        return new ResponseEntity<>(notificationService.createNotification(request), HttpStatus.CREATED);
    }

    @PatchMapping("/{userId}/notifications/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long userId, @PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(userId, id));
    }
}
