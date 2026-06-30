package org.training.user.service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.training.user.service.exception.EmailSendingException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendOtp(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Xác thực email Banking App");
        message.setText("Xin chào,\n\n"
                + "Mã OTP xác thực email của bạn là: " + otp + "\n\n"
                + "Mã này có hiệu lực trong 5 phút.\n"
                + "Vui lòng không chia sẻ mã này cho người khác.\n\n"
                + "Banking App");

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.error("Không thể gửi email OTP tới {}: {}", email, exception.getMessage());
            throw new EmailSendingException(
                    "Không thể gửi email OTP. Vui lòng thử gửi lại sau.", exception);
        }
    }
}
