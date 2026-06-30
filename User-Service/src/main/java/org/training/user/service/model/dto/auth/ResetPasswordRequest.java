package org.training.user.service.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordRequest {
    @NotBlank(message = "Email is required")
    private String email;
    @NotBlank(message = "OTP is required")
    private String otp;
    @NotBlank(message = "New password is required")
    private String newPassword;
}
