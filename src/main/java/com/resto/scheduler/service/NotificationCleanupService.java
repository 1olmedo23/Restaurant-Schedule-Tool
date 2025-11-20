package com.resto.scheduler.service;

import com.resto.scheduler.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class NotificationCleanupService {

    private final NotificationRepository notificationRepo;

    public NotificationCleanupService(NotificationRepository notificationRepo) {
        this.notificationRepo = notificationRepo;
    }

    /**
     * Real production cleanup:
     * Runs every day at 3:00 AM server time and deletes notifications
     * older than 14 days.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void scheduledCleanup() {
        deleteNotificationsOlderThanDays(14);
    }

    /**
     * Reusable cleanup method – can be called from tests or controllers.
     *
     * @param days how many days old a notification must be (based on createdAt)
     *             to be deleted. Example:
     *             - 14 = delete anything older than 14 days
     *             - 0  = delete anything older than "right now" (i.e., everything)
     */
    @Transactional
    public long deleteNotificationsOlderThanDays(long days) {
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        return notificationRepo.deleteByCreatedAtBefore(cutoff);
    }
}
