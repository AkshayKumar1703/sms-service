package com.example.sms_service.controller;

import org.springframework.stereotype.Component;

@Component
public class ValidateMobileNumber {
    //Method to check if a given number is valid or not
    private static final String MOBILE_REGEX = "^\\+[1-9]\\d{7,14}$";


    String validateMobile(String number) {
        if (number != null && number.matches(MOBILE_REGEX)) {
            return "Valid Mobile Number";
        }
        return "Invalid Mobile Number";
    }
}
