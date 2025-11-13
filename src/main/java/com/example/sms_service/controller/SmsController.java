package com.example.sms_service.controller;

import com.example.sms_service.dto.SmsRequest;
import com.example.sms_service.service.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sms")
public class SmsController {

    @Autowired
    private SmsService smsService;

    @PostMapping
    public String sendSms(@RequestBody SmsRequest smsRequest) {
        return smsService.sendSms(smsRequest.getPhoneNumber(), smsRequest.getMessage());
    }
}
