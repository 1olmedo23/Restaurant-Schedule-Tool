package com.resto.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RestoSchedulerApplication {
  public static void main(String[] args) {
    SpringApplication.run(RestoSchedulerApplication.class, args);
  }
}