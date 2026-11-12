package com.example.sms_service.model;

public record QueuedSms(String id, String phoneNumber, String message, String priority) {

}
