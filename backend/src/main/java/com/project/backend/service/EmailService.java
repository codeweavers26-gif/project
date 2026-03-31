package com.project.backend.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {
private final JavaMailSender mailSender;

    public void sendOtp(String email, String otp) {
      
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setTo(email);
            message.setSubject("Your OTP Code");
            message.setText(
                    "Your OTP is: " + otp + "\n\n" +
                    "This OTP will expire in 5 minutes.\n" +
                    "Do not share it with anyone."
            );

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send email OTP", e);
        }
    }
}