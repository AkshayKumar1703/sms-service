package com.example.sms_service.controller;

import com.example.sms_service.model.SmsMessage;
import com.example.sms_service.service.SmsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sms")
public class SmsController {

    private final SmsService smsService;

    public SmsController(SmsService smsService) {
        this.smsService = smsService;
    }

    @PostMapping("/send")
    public ResponseEntity<SmsMessage> sendSms(@RequestBody Map<String, String> request) {
        String phone = request.get("phoneNumber");
        String message = request.get("message");

        return ResponseEntity.ok(smsService.sendSms(phone, message));
    }
}
