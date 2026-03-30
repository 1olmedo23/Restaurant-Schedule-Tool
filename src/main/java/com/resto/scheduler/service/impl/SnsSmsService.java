package com.resto.scheduler.service.impl;

import com.resto.scheduler.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SnsException;

public class SnsSmsService implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(SnsSmsService.class);

    private final SnsClient snsClient;

    public SnsSmsService(SnsClient snsClient) {
        this.snsClient = snsClient;
    }

    @Override
    public void send(String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            log.warn("SMS skipped: missing phone number. Message={}", message);
            return;
        }

        try {
            PublishRequest request = PublishRequest.builder()
                    .phoneNumber(phoneNumber)
                    .message(message)
                    .build();

            var response = snsClient.publish(request);
            log.info("SMS sent via AWS SNS. MessageId={}, Phone={}", response.messageId(), phoneNumber);

        } catch (InvalidParameterException ex) {
            log.error("AWS SNS rejected SMS request for phone {}. Ensure E.164 format like +12065550101. Error={}",
                    phoneNumber, ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorMessage() : ex.getMessage());
        } catch (SnsException ex) {
            log.error("AWS SNS failed to send SMS to {}. Error={}",
                    phoneNumber, ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorMessage() : ex.getMessage());
        }
    }
}