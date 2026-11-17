package com.example.sms_service.controller;

import com.example.sms_service.model.Priority;
import com.example.sms_service.model.SmsMessage;
import com.example.sms_service.repository.SmsMessageRepository;
import com.example.sms_service.requestDTO.SendSmsRequest;
import com.example.sms_service.service.SmsQueueService;
import com.example.sms_service.service.SmsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import java.util.List;

@RestController
@RequestMapping("/api/sms")
@Validated
public class SmsController {
  // constructor injection of smsService, smsQueueService, repository
  private final SmsService smsService;
  private final SmsQueueService smsQueueService;
  private final SmsMessageRepository repository;

  public SmsController(
      SmsService smsService, SmsQueueService smsQueueService, SmsMessageRepository repository) {
    this.smsService = smsService;
    this.smsQueueService = smsQueueService;
    this.repository = repository;
  }

  @PostMapping("/send")
  public SmsMessage send(@Valid @RequestBody SendSmsRequest req) {
    return smsService.sendSms(req.phoneNumber(), req.message());
  }

  @PostMapping("/queue")
  public ResponseEntity<String> queue(@Valid @RequestBody SendSmsRequest req) {
    Priority p = req.priority() != null ? req.priority() : Priority.MEDIUM;
    smsQueueService.enqueue(req.phoneNumber(), req.message(), p);
    return ResponseEntity.accepted().body("Queued with priority " + p);
  }

  @PostMapping("/queue/process")
  public List<SmsMessage> process(@RequestParam(defaultValue = "5") @Min(1) @Max(50) int count) {
    return smsQueueService.processQueue(count);
  }

  @GetMapping("/{id}")
  public ResponseEntity<SmsMessage> getById(@PathVariable Long id) {
    SmsMessage sms = smsService.getById(id);
    return sms != null ? ResponseEntity.ok(sms) : ResponseEntity.notFound().build();
  }

  @GetMapping
  public Page<SmsMessage> getAll(Pageable pageable) {
    return repository.findAll(pageable);
  }
}
