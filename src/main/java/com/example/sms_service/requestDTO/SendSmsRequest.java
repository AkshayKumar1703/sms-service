package com.example.sms_service.requestDTO;

import com.example.sms_service.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SendSmsRequest(
    @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Invalid mobile number")
        String phoneNumber,
    @NotBlank @Size(max = 1600) String message,
    Priority priority) {} // null -> MEDIUM; a bad value gives a 400 automatically
