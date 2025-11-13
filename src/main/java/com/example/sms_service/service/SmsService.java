package com.example.sms_service.service;

import org.springframework.stereotype.Service;

@Service
public class SmsService {

    public String sendSms(String phoneNumber, String message) {
        // For now, we’ll just simulate sending an SMS
        System.out.println("Sending SMS to " + phoneNumber + ": " + message);
        return "SMS sent successfully to " + phoneNumber;
    }
}
