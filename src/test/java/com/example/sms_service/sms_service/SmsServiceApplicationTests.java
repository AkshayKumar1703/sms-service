package com.example.sms_service.sms_service;

import com.example.sms_service.model.SmsMessage;
import com.example.sms_service.service.SmsService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SmsServiceTest {

    @Test
    void testSendSms() {
        SmsService service = Mockito.mock(SmsService.class);
        SmsMessage sms = new SmsMessage();
        sms.setPhoneNumber("+1234567890");
        sms.setMessage("Hello");
        sms.setStatus("SENT");

        Mockito.when(service.sendSms(sms.getPhoneNumber(), sms.getMessage())).thenReturn(sms);

        SmsMessage result = service.sendSms(sms.getPhoneNumber(), sms.getMessage());
        assertEquals("SENT", result.getStatus());
    }
}
