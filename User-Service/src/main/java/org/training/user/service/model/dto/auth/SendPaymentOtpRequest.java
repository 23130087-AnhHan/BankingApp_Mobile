package org.training.user.service.model.dto.auth;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class SendPaymentOtpRequest {
    @Email
    @NotBlank
    private String email;
}
