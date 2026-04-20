package com.project.backend.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Async
    public void sendOtp(String to, String otp) {

        if (fromEmail.isEmpty()) {
            log.warn("Mail not configured (spring.mail.username empty), skipping OTP email to {}", to);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Your OTP - Rich and Retired");
            helper.setText(buildOtpHtml(otp), true);

            mailSender.send(message);
            log.info("OTP email sent to {}", to);

        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", to, e.getMessage(), e);
        }
    }

    private String buildOtpHtml(String otp) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:32px;border:1px solid #eee;border-radius:8px;">
                  <h2 style="color:#0a0a0a;letter-spacing:2px;text-transform:uppercase;font-weight:300;">Rich &amp; Retired</h2>
                  <p style="color:#555;font-size:15px;">Your one-time password (OTP) is:</p>
                  <div style="font-size:36px;font-weight:bold;letter-spacing:8px;text-align:center;padding:20px 0;color:#0a0a0a;">
                    %s
                  </div>
                  <p style="color:#888;font-size:13px;">This OTP is valid for <strong>5 minutes</strong>. Do not share it with anyone.</p>
                  <hr style="border:none;border-top:1px solid #eee;margin:24px 0"/>
                  <p style="color:#bbb;font-size:11px;text-align:center;">Rich and Retired &nbsp;|&nbsp; richnretired.com</p>
                </div>
                """.formatted(otp);
    }
}
