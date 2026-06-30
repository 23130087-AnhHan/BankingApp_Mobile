package org.training.user.service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.training.user.service.exception.GlobalException;
import org.training.user.service.model.entity.User;
import org.training.user.service.repository.UserRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OtpService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_BOUND = 1_000_000;

    private final UserRepository userRepository;
    private final EmailService emailService;

    public String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(OTP_BOUND));
    }

    public LocalDateTime generateExpiredTime() {
        return LocalDateTime.now().plusMinutes(5);
    }

    public boolean isExpired(LocalDateTime expiredAt) {
        return expiredAt == null || LocalDateTime.now().isAfter(expiredAt);
    }

    @Transactional
    public String verifyEmailOtp(String email, String otp) {
        String normalizedEmail = requireValue(email, "Email không được để trống")
                .toLowerCase(Locale.ROOT);
        String normalizedOtp = requireValue(otp, "OTP không được để trống");
        User user = findByEmail(normalizedEmail);

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return "Email đã được xác thực trước đó";
        }
        if (isExpired(user.getEmailOtpExpiredAt())) {
            throw badRequest("OTP đã hết hạn");
        }
        if (!normalizedOtp.equals(user.getEmailOtp())) {
            throw badRequest("OTP không chính xác");
        }

        user.setEmailVerified(true);
        user.setEmailOtp(null);
        user.setEmailOtpExpiredAt(null);
        userRepository.save(user);
        return "Xác thực email thành công";
    }

    @Transactional
    public String resendEmailOtp(String email) {
        String normalizedEmail = requireValue(email, "Email không được để trống")
                .toLowerCase(Locale.ROOT);
        User user = findByEmail(normalizedEmail);

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return "Email đã được xác thực, không cần gửi lại OTP";
        }

        String otp = generateOtp();
        user.setEmailOtp(otp);
        user.setEmailOtpExpiredAt(generateExpiredTime());
        userRepository.save(user);
        emailService.sendOtp(user.getEmailId(), otp);
        return "Đã gửi lại OTP. Vui lòng kiểm tra email";
    }

    private User findByEmail(String email) {
        return userRepository.findByEmailIdIgnoreCase(email)
                .orElseThrow(() -> badRequest("Email không tồn tại"));
    }

    private String requireValue(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw badRequest(message);
        }
        return value.trim();
    }

    private GlobalException badRequest(String message) {
        return new GlobalException(message, "400");
    }
}
