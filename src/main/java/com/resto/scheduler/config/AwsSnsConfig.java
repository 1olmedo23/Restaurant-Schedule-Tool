package com.resto.scheduler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
public class AwsSnsConfig {

    @Bean
    public SnsClient snsClient(@Value("${aws.region}") String region) {
        return SnsClient.builder()
                .region(Region.of(region))
                .build();
    }
}