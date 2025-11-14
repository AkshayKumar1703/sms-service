package com.example.sms_service.service;

import com.example.sms_service.config.SmsApiConfig;
import com.example.sms_service.model.SmsMessage;
import com.example.sms_service.repository.SmsMessageRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class SmsService {

    private final SmsMessageRepository repository;
    private final SmsApiConfig smsApiConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    public SmsService(SmsMessageRepository repository, SmsApiConfig smsApiConfig) {
        this.repository = repository;
        this.smsApiConfig = smsApiConfig;
    }

    public SmsMessage sendSms(String to, String body) {
        SmsMessage sms = new SmsMessage();
        sms.setPhoneNumber(to);
        sms.setMessage(body);

        try {
            // Prepare request body
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("to", to);
            requestBody.put("message", body);
            requestBody.put("from", smsApiConfig.getFrom());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(smsApiConfig.getToken());

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            // Send POST request to SMSAPI
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.smsapi.com/sms.do", request, String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                sms.setStatus("SENT");
            } else {
                sms.setStatus("FAILED");
            }

        } catch (Exception e) {
            sms.setStatus("FAILED");
        }

        return repository.save(sms);
    }
}
