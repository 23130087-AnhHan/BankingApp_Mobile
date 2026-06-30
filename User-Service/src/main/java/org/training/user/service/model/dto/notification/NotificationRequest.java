package org.training.user.service.model.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    @NotNull(message = "Thiếu mã khách hàng nhận thông báo")
    private Long userId;

    @NotBlank(message = "Tiêu đề thông báo không được để trống")
    @Size(max = 120, message = "Tiêu đề thông báo quá dài")
    private String title;

    @NotBlank(message = "Nội dung thông báo không được để trống")
    @Size(max = 500, message = "Nội dung thông báo quá dài")
    private String message;

    @NotBlank(message = "Loại thông báo không được để trống")
    @Size(max = 60, message = "Loại thông báo quá dài")
    private String type;

    @Size(max = 80, message = "Mã tham chiếu quá dài")
    private String referenceId;
}
