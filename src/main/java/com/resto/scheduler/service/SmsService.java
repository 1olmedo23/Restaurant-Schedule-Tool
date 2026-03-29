package com.resto.scheduler.service;

public interface SmsService {
    void send(String phoneNumber, String message);
}