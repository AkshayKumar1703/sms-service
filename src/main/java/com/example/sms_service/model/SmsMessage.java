package com.example.sms_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmsMessage implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String phoneNumber;
  private String message;
  private String status;
  private String twilioSid;

  @Column(length = 500)
  private String errorMessage;

  private LocalDateTime createdAt = LocalDateTime.now();
}
