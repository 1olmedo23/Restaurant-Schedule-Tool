package com.resto.scheduler.repository;

import com.resto.scheduler.model.AppUser;
import com.resto.scheduler.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Existing methods you had
    List<Notification> findByRecipientOrderByCreatedAtDesc(AppUser user);
    long countByRecipientAndReadIsFalse(AppUser user);
    List<Notification> findByRecipientOrderByIdDesc(AppUser recipient);

    // For unread/read split on the /notifications page
    List<Notification> findByRecipientAndReadIsFalseOrderByCreatedAtDesc(AppUser recipient);
    List<Notification> findByRecipientAndReadIsTrueOrderByCreatedAtDesc(AppUser recipient);

    // NEW: bulk delete old notifications
    long deleteByCreatedAtBefore(Instant cutoff);
}
