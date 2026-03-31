package com.project.backend.service;

import org.springframework.stereotype.Service;

@Service
public class SmsService {

    public void sendOtp(String phone, String otp) {

        System.out.println("SMS OTP to " + phone + " : " + otp);
    }
}