package com.resto.scheduler.service.impl;

import com.resto.scheduler.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LoggingSmsService implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsService.class);

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;

    @Override
    public void send(String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            log.warn("SMS skipped: missing phone number. Message={}", message);
            return;
        }

        if (!smsEnabled) {
            log.info("SMS disabled. Would have sent to {}: {}", phoneNumber, message);
            return;
        }

        // Temporary behavior for now:
        // even when enabled=true, we only log until Twilio is added.
        log.info("SMS provider not wired yet. Would send to {}: {}", phoneNumber, message);
    }
}