package com.example.sms_service.model;

public record QueuedSms(
    String id, String phoneNumber, String message, Priority priority, int attempts) {
  public QueuedSms retry() {
    return new QueuedSms(id, phoneNumber, message, priority, attempts + 1);
  }
}
