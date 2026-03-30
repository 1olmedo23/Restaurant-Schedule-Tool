package com.resto.scheduler.controller;

import com.resto.scheduler.service.SmsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SmsTestController {

    private final SmsService smsService;

    public SmsTestController(SmsService smsService) {
        this.smsService = smsService;
    }

    @GetMapping("/test-sms")
    public String testSms(@RequestParam String phone) {
        smsService.send(phone, "Test SMS from Resto Scheduler");
        return "SMS request sent. Check logs and phone.";
    }
}