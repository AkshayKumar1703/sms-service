package com.example.sms_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SmsApiConfig {

    @Value("${smsapi.token}")
    private String token;

    @Value("${smsapi.from}")
    private String from;

    public String getToken() {
        return token;
    }

    public String getFrom() {
        return from;
    }
}
