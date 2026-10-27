package com.example.sms_service.controller;

import com.example.sms_service.model.SmsMessage;
import com.example.sms_service.repository.SmsMessageRepository;
import com.example.sms_service.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
@RestController
@Slf4j
@RequestMapping("/api/sms")
public class SmsController {

    private final SmsService smsService;
    private final SmsMessageRepository repository;

    public SmsController(SmsService smsService, SmsMessageRepository repository) {
        this.smsService = smsService;
        this.repository = repository;
    }

    @PostMapping("/send")
    public ResponseEntity<SmsMessage> sendSms(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(
                smsService.sendSms(request.get("phoneNumber"), request.get("message")));
    }

    @GetMapping
    public List<SmsMessage> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SmsMessage> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
