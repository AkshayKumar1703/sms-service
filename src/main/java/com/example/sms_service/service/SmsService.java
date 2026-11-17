package com.example.sms_service.service;

import com.example.sms_service.model.SmsMessage;
import com.example.sms_service.repository.SmsMessageRepository;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

  private static final Logger log = LoggerFactory.getLogger(SmsService.class);

  @Value("${twilio.from-phone}")
  private String fromPhone;
    private final MeterRegistry registry;
  private final SmsMessageRepository repository;
  public SmsService(SmsMessageRepository repository, MeterRegistry registry) {

    this.repository = repository;
    this.registry = registry;
  }

  @Cacheable(value = "sms", key = "#id", unless = "#result == null")
  public SmsMessage getById(Long id) {
    log.info("Cache miss: loading SMS {} from database", id);
    return repository.findById(id).orElse(null);
  }

  @CachePut(value = "sms", key = "#result.id")
  public SmsMessage sendSms(String to, String body) {
    log.info("Sending SMS to {}", to);

    SmsMessage sms = new SmsMessage();
    sms.setPhoneNumber(to);
    sms.setMessage(body);
      Timer.Sample sample = Timer.start(registry);
    try {
      Message result =
          Message.creator(new PhoneNumber(to), new PhoneNumber(fromPhone), body).create();
      log.debug("Request received: to = {}, bodyLength = {}", to, body.length());
      sms.setStatus("SENT");
      sms.setTwilioSid(result.getSid());
    } catch (Exception e) {
      log.error("Failed to send SMS to {}", to, e);
      sms.setStatus("FAILED");
      sms.setErrorMessage(e.getMessage());
    }
    finally {
          sample.stop(Timer.builder("sms.twilio.latency")
                  .publishPercentileHistogram()
                  .register(registry));
      }
      registry.counter("sms.sent", "status", sms.getStatus()).increment();
    return repository.save(sms);
  }
}
