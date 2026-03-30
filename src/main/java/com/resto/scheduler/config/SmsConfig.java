package com.resto.scheduler.config;

import com.resto.scheduler.service.SmsService;
import com.resto.scheduler.service.impl.LoggingSmsService;
import com.resto.scheduler.service.impl.SnsSmsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
public class SmsConfig {

    @Bean
    public SmsService smsService(
            @Value("${app.sms.provider:logging}") String provider,
            LoggingSmsService loggingSmsService,
            SnsClient snsClient
    ) {
        if ("sns".equalsIgnoreCase(provider)) {
            return new SnsSmsService(snsClient);
        }

        return loggingSmsService;
    }
}