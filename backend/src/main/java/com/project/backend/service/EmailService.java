package com.project.backend.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

    @Value("${sendgrid.api-key:}")
    private String apiKey;

    @Value("${sendgrid.from-email:}")
    private String fromEmail;

    @Async
    public void sendOtp(String to, String otp) {

        if (apiKey.isEmpty() || fromEmail.isEmpty()) {
            log.warn("SendGrid not configured, skipping email to {}", to);
            return;
        }

        try {
            Email from = new Email(fromEmail);
            Email recipient = new Email(to);

            String subject = "Your OTP - Rich and Retired";
            String body = buildOtpHtml(otp);

            Mail mail = new Mail(from, subject, recipient, new Content("text/html", body));

            SendGrid sg = new SendGrid(apiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() == 202) {
                log.info("OTP email sent to {} via SendGrid", to);
            } else {
                log.error("SendGrid failed: status={}, body={}", response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            log.error("SendGrid exception for {}: {}", to, e.getMessage(), e);
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
