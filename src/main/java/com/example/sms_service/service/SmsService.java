package com.example.sms_service.service;

import com.example.sms_service.model.SmsMessage;
import com.example.sms_service.repository.SmsMessageRepository;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    @Value("${twilio.from-phone}")
    private String fromPhone;

    private final SmsMessageRepository repository;

    public SmsService(SmsMessageRepository repository) {
        this.repository = repository;
    }

    public SmsMessage sendSms(String to, String body) {
        SmsMessage sms = new SmsMessage();
        sms.setPhoneNumber(to);
        sms.setMessage(body);

        try {
            Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(fromPhone),
                    body
            ).create();

            sms.setStatus("SENT");
        } catch (Exception e) {
            sms.setStatus("FAILED");
        }

        return repository.save(sms);
    }
}
