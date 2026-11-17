package com.example.sms_service.repository;

import com.example.sms_service.model.SmsMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SmsMessageRepository extends JpaRepository<SmsMessage, Long> {
  Optional<SmsMessage> findByTwilioSid(String twilioSid);
}
