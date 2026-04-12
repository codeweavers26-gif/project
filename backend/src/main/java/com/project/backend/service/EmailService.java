package com.project.backend.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    @Value("${sendgrid.api-key}")
    private String apiKey;

    @Value("${sendgrid.from-email}")
    private String fromEmail;

    @Async
    public void sendOtp(String to, String otp) {

        try {
            Email from = new Email(fromEmail);
            Email recipient = new Email(to);

            String subject = "Your OTP Code";
            String body = "<strong>Your OTP is: " + otp + "</strong><br/>Valid for 5 minutes.";

            Mail mail = new Mail(from, subject, recipient, new Content("text/html", body));

            SendGrid sg = new SendGrid(apiKey);
            Request request = new Request();

            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() != 202) {
                log.error("❌ SendGrid failed: status={}, body={}",
                        response.getStatusCode(),
                        response.getBody());
                throw new RuntimeException("Email send failed");
            }

            log.info("✅ OTP email sent to {}", to);

        } catch (Exception e) {
            log.error("❌ SendGrid exception for {}", to, e);

            // 🔥 DO NOT BREAK USER FLOW
            // Optionally: save to retry queue / DB
        }
    }
}