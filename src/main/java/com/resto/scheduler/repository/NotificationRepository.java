package com.resto.scheduler.repository;

import com.resto.scheduler.model.AppUser;
import com.resto.scheduler.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientOrderByCreatedAtDesc(AppUser user);
    long countByRecipientAndReadIsFalse(AppUser user);
}
