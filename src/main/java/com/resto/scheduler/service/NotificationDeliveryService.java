package com.resto.scheduler.service;

import com.resto.scheduler.model.AppUser;
import com.resto.scheduler.model.Notification;
import com.resto.scheduler.model.enums.NotificationType;
import com.resto.scheduler.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationDeliveryService {

    private final NotificationRepository notificationRepository;
    private final SmsService smsService;

    public NotificationDeliveryService(NotificationRepository notificationRepository,
                                       SmsService smsService) {
        this.notificationRepository = notificationRepository;
        this.smsService = smsService;
    }

    @Transactional
    public void notifyInApp(AppUser recipient, NotificationType type, String payload) {
        if (recipient == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setPayload(payload);
        notificationRepository.save(notification);
    }

    @Transactional
    public void notifyInAppAndSms(AppUser recipient,
                                  NotificationType type,
                                  String payload,
                                  String smsMessage) {
        if (recipient == null) {
            return;
        }

        notifyInApp(recipient, type, payload);
        smsService.send(recipient.getPhoneNumber(), smsMessage);
    }
}
