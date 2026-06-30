package org.training.user.service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.training.user.service.model.dto.auth.AuthResponse;
import org.training.user.service.model.dto.auth.LoginRequest;
import org.training.user.service.model.dto.auth.RefreshTokenRequest;
import org.training.user.service.model.dto.auth.ForgotPasswordRequest;
import org.training.user.service.model.dto.auth.ResetPasswordRequest;
import org.training.user.service.model.dto.auth.ResendEmailOtpRequest;
import org.training.user.service.model.dto.auth.SendPaymentOtpRequest;
import org.training.user.service.model.dto.auth.VerifyEmailOtpRequest;
import org.training.user.service.model.dto.auth.VerifyPaymentOtpRequest;
import org.training.user.service.model.dto.response.Response;
import org.training.user.service.model.dto.response.AvailabilityResponse;
import org.training.user.service.service.AuthenticationService;
import org.training.user.service.service.OtpService;
import org.training.user.service.service.UserAvailabilityService;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/users/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final OtpService otpService;
    private final UserAvailabilityService userAvailabilityService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authenticationService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authenticationService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authenticationService.requestPasswordReset(request.getEmail());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Response> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authenticationService.resetPassword(request);
        return ResponseEntity.ok(Response.builder()
                .responseCode("200")
                .responseMessage("Mật khẩu đã được cập nhật thành công")
                .build());
    }

    @PostMapping("/verify-email-otp")
    public ResponseEntity<Response> verifyEmailOtp(
            @Valid @RequestBody VerifyEmailOtpRequest request) {
        return ResponseEntity.ok(Response.builder()
                .responseCode("200")
                .responseMessage(otpService.verifyEmailOtp(request.getEmail(), request.getOtp()))
                .build());
    }

    @PostMapping("/resend-email-otp")
    public ResponseEntity<Response> resendEmailOtp(
            @Valid @RequestBody ResendEmailOtpRequest request) {
        return ResponseEntity.ok(Response.builder()
                .responseCode("200")
                .responseMessage(otpService.resendEmailOtp(request.getEmail()))
                .build());
    }

    @GetMapping("/check-email")
    public ResponseEntity<AvailabilityResponse> checkEmail(@RequestParam String email) {
        return ResponseEntity.ok(userAvailabilityService.checkEmail(email));
    }

    @GetMapping("/check-phone")
    public ResponseEntity<AvailabilityResponse> checkPhone(@RequestParam String phone) {
        return ResponseEntity.ok(userAvailabilityService.checkPhone(phone));
    }

    @PostMapping("/payment-otp/send")
    public ResponseEntity<Response> sendPaymentOtp(@Valid @RequestBody SendPaymentOtpRequest request) {
        return ResponseEntity.ok(Response.builder()
                .responseCode("200")
                .responseMessage(otpService.sendPaymentOtp(request.getEmail()))
                .build());
    }

    @PostMapping("/payment-otp/verify")
    public ResponseEntity<Response> verifyPaymentOtp(@Valid @RequestBody VerifyPaymentOtpRequest request) {
        return ResponseEntity.ok(Response.builder()
                .responseCode("200")
                .responseMessage(otpService.verifyPaymentOtp(request.getEmail(), request.getOtp()))
                .build());
    }
}
