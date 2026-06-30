package org.training.user.service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.training.user.service.exception.GlobalException;
import org.training.user.service.model.dto.response.AvailabilityResponse;
import org.training.user.service.repository.UserRepository;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserAvailabilityService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^0[0-9]{9}$");

    private final UserRepository userRepository;

    public AvailabilityResponse checkEmail(String email) {
        String normalizedEmail = email == null
                ? ""
                : email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.isEmpty()) {
            throw badRequest("Email không được để trống");
        }
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw badRequest("Email không đúng định dạng");
        }

        boolean exists = userRepository.existsByEmailIdIgnoreCase(normalizedEmail);
        return AvailabilityResponse.builder()
                .exists(exists)
                .message(exists ? "Email đã được sử dụng" : "Email có thể sử dụng")
                .build();
    }

    public AvailabilityResponse checkPhone(String phone) {
        String normalizedPhone = phone == null ? "" : phone.trim();
        if (normalizedPhone.isEmpty()) {
            throw badRequest("Số điện thoại không được để trống");
        }
        if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            throw badRequest("Số điện thoại phải bắt đầu bằng 0 và có đúng 10 chữ số");
        }

        boolean exists = userRepository.existsByContactNo(normalizedPhone);
        return AvailabilityResponse.builder()
                .exists(exists)
                .message(exists
                        ? "Số điện thoại đã được sử dụng"
                        : "Số điện thoại có thể sử dụng")
                .build();
    }

    private GlobalException badRequest(String message) {
        return new GlobalException(message, "400");
    }
}
