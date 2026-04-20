package com.project.backend.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsService {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.from-number:}")
    private String fromNumber;

    @Async
    public void sendOtp(String phone, String otp) {

        if (accountSid.isEmpty() || authToken.isEmpty() || fromNumber.isEmpty()) {
            log.warn("Twilio not configured, skipping SMS OTP to {}", phone);
            return;
        }

        try {
            Twilio.init(accountSid, authToken);

            // Add +91 country code for Indian numbers if not already prefixed
            String toNumber = phone.startsWith("+") ? phone : "+91" + phone;

            Message message = Message.creator(
                    new PhoneNumber(toNumber),
                    new PhoneNumber(fromNumber),
                    "Your Rich & Retired OTP is: " + otp + ". Valid for 5 minutes. Do not share with anyone."
            ).create();

            log.info("OTP SMS sent to {} via Twilio, SID={}", toNumber, message.getSid());

        } catch (Exception e) {
            log.error("Twilio SMS failed for {}: {}", phone, e.getMessage(), e);
        }
    }
}
