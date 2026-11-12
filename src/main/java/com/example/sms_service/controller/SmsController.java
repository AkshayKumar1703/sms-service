package com.example.sms_service.controller;

import com.example.sms_service.model.Priority;
import com.example.sms_service.model.SmsMessage;
import com.example.sms_service.repository.SmsMessageRepository;
import com.example.sms_service.service.SmsQueueService;
import com.example.sms_service.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
@RestController
@Slf4j

@RequestMapping("/api/sms")
public class SmsController {

    private final SmsService smsService;
    private final SmsQueueService smsQueueService;
    private final SmsMessageRepository repository;
    private final ValidateMobileNumber ValidateMobileNumber;
    public SmsController(SmsService smsService, SmsQueueService smsQueueService, SmsMessageRepository repository, ValidateMobileNumber validateMobileNumber) {
        this.smsService = smsService;
        this.smsQueueService = smsQueueService;
        this.repository = repository;
        this.ValidateMobileNumber = validateMobileNumber;
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendSms(@RequestBody Map<String, String> request) {
        String phone = request.get("phoneNumber");
        String result =ValidateMobileNumber.validateMobile(phone);

        if (result.equals("Invalid Mobile Number")) {
            log.warn("Invalid phone number: {}", phone);
            return ResponseEntity.badRequest().body(result);
        }

        log.info("Phone number is valid: {}", phone);
        return ResponseEntity.ok(
                smsService.sendSms(phone, request.get("message")));
    }

    @GetMapping
    public List<SmsMessage> getAll() {
        return repository.findAll();
    }

//    @GetMapping("/{id}")
//    public ResponseEntity<SmsMessage> getById(@PathVariable Long id) {
//        return repository.findById(id)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }
    @GetMapping("/{id}")
    public ResponseEntity<SmsMessage> getById(@PathVariable Long id) {
        SmsMessage sms = smsService.getById(id);
        return sms != null ? ResponseEntity.ok(sms) : ResponseEntity.notFound().build();
    }
    @PostMapping("/queue")
    public ResponseEntity<?> queueSms(@RequestBody Map<String, String> request) {
        String phone = request.get("phoneNumber");

        if (ValidateMobileNumber.validateMobile(phone).equals("Invalid Mobile Number")) {   // your existing validation
            log.warn("Invalid phone number: {}", phone);
            return ResponseEntity.badRequest().body("Invalid Mobile Number");
        }

        Priority priority;
        try {
            priority = Priority.valueOf(request.getOrDefault("priority", "MEDIUM").toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid priority: {}", request.get("priority"));
            return ResponseEntity.badRequest().body("Priority must be HIGH, MEDIUM or LOW");
        }

        smsQueueService.enqueue( phone, request.get("message"), priority);
        return ResponseEntity.accepted().body("Queued with priority " + priority);
    }

    @PostMapping("/queue/process")
    public ResponseEntity<List<SmsMessage>> processQueue(@RequestParam(defaultValue = "5") int count) {
        return ResponseEntity.ok(smsQueueService.processQueue(count));
    }
}
