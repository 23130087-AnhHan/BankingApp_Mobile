package org.training.user.service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.training.user.service.exception.AuthenticationFailedException;
import org.training.user.service.model.dto.auth.AuthResponse;
import org.training.user.service.model.dto.auth.LoginRequest;
import org.training.user.service.model.dto.auth.ResetPasswordRequest;
import org.training.user.service.model.entity.User;
import org.training.user.service.repository.UserRepository;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.config.keycloak.server-url}")
    private String serverUrl;
    @Value("${app.config.keycloak.realm}")
    private String realm;
    @Value("${app.config.keycloak.client-id}")
    private String clientId;
    @Value("${app.config.keycloak.client-secret}")
    private String clientSecret;

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIdIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new AuthenticationFailedException("Email or password is incorrect"));
        Map<String, Object> token = requestToken(form("password", request.getEmail().trim(), request.getPassword()));
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new AuthenticationFailedException("Vui lòng xác thực email trước khi đăng nhập");
        }
        return toResponse(token, user);
    }

    public AuthResponse refresh(String refreshToken) {
        MultiValueMap<String, String> form = baseForm("refresh_token");
        form.add("refresh_token", refreshToken);
        Map<String, Object> token = requestToken(form);
        String authId = readSubject(token.get("access_token"));
        User user = userRepository.findUserByAuthId(authId)
                .orElseThrow(() -> new AuthenticationFailedException("User session is no longer available"));
        return toResponse(token, user);
    }

    public void logout(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        try {
            restTemplate.exchange(logoutUrl(), HttpMethod.POST, new HttpEntity<>(form, headers), Void.class);
        } catch (HttpStatusCodeException ex) {
            throw new AuthenticationFailedException("Unable to close authentication session");
        }
    }

    public void requestPasswordReset(String email) {
        userRepository.findByEmailIdIgnoreCase(email.trim())
                .ifPresent(user -> {
                    String otp = otpService.generateOtp();
                    user.setEmailOtp(otp);
                    user.setEmailOtpExpiredAt(otpService.generateExpiredTime());
                    userRepository.save(user);
                    emailService.sendOtp(user.getEmailId(), otp);
                });
    }

    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmailIdIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new AuthenticationFailedException("Email not found"));

        if (otpService.isExpired(user.getEmailOtpExpiredAt())) {
            throw new AuthenticationFailedException("OTP has expired");
        }
        if (!request.getOtp().equals(user.getEmailOtp())) {
            throw new AuthenticationFailedException("Invalid OTP");
        }

        org.keycloak.representations.idm.UserRepresentation keycloakUser = keycloakService.readUser(user.getAuthId());
        org.keycloak.representations.idm.CredentialRepresentation cred = new org.keycloak.representations.idm.CredentialRepresentation();
        cred.setType(org.keycloak.representations.idm.CredentialRepresentation.PASSWORD);
        cred.setValue(request.getNewPassword());
        cred.setTemporary(false);
        keycloakUser.setCredentials(java.util.Collections.singletonList(cred));
        keycloakService.updateUser(keycloakUser);

        user.setEmailOtp(null);
        user.setEmailOtpExpiredAt(null);
        userRepository.save(user);
    }

    private MultiValueMap<String, String> form(String grantType, String username, String password) {
        MultiValueMap<String, String> form = baseForm(grantType);
        form.add("username", username);
        form.add("password", password);
        form.add("scope", "openid");
        return form;
    }

    private MultiValueMap<String, String> baseForm(String grantType) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", grantType);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        return form;
    }

    private Map<String, Object> requestToken(MultiValueMap<String, String> form) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    tokenUrl(), HttpMethod.POST, new HttpEntity<>(form, headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            if (response.getBody() == null) {
                throw new AuthenticationFailedException("Authentication server returned an empty response");
            }
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            throw new AuthenticationFailedException("Email or password is incorrect, or the account is disabled");
        }
    }

    private AuthResponse toResponse(Map<String, Object> token, User user) {
        String displayName = user.getUserProfile() == null ? user.getEmailId()
                : (user.getUserProfile().getFirstName() + " " + user.getUserProfile().getLastName()).trim();
        return AuthResponse.builder()
                .accessToken(string(token.get("access_token")))
                .refreshToken(string(token.get("refresh_token")))
                .expiresIn(number(token.get("expires_in")))
                .tokenType(string(token.get("token_type")))
                .userId(user.getUserId())
                .email(user.getEmailId())
                .displayName(displayName)
                .status(user.getStatus())
                .build();
    }

    private String readSubject(Object encodedToken) {
        try {
            String[] parts = string(encodedToken).split("\\.");
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload).path("sub").asText();
        } catch (Exception ex) {
            throw new AuthenticationFailedException("Invalid access token returned by authentication server");
        }
    }

    private String tokenUrl() {
        return serverUrl.replaceAll("/+$", "") + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    private String logoutUrl() {
        return serverUrl.replaceAll("/+$", "") + "/realms/" + realm + "/protocol/openid-connect/logout";
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static Long number(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : Long.valueOf(string(value));
    }
}
