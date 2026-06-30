package org.training.user.service.model.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;

    private Long userId;

    private String title;

    private String message;

    private String type;

    private String referenceId;

    private Boolean read;

    private LocalDateTime createdAt;
}
