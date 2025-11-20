package com.resto.scheduler.controller;

import com.resto.scheduler.model.AppUser;
import com.resto.scheduler.model.Notification;
import com.resto.scheduler.repository.AppUserRepository;
import com.resto.scheduler.repository.NotificationRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepo;
    private final AppUserRepository userRepo;

    public NotificationController(NotificationRepository notificationRepo,
                                  AppUserRepository userRepo) {
        this.notificationRepo = notificationRepo;
        this.userRepo = userRepo;
    }

    /**
     * Show notifications split into Unread + Previously read.
     */
    @GetMapping
    public String list(Authentication auth, Model model) {
        AppUser me = userRepo.findByUsername(auth.getName()).orElseThrow();

        List<Notification> unread =
                notificationRepo.findByRecipientAndReadIsFalseOrderByCreatedAtDesc(me);

        List<Notification> read =
                notificationRepo.findByRecipientAndReadIsTrueOrderByCreatedAtDesc(me);

        model.addAttribute("me", me);
        model.addAttribute("unread", unread);
        model.addAttribute("read", read);
        model.addAttribute("active", "notifications");

        return "notifications";
    }

    /**
     * Mark a single notification as read for this user.
     */
    @PostMapping("/{id}/read")
    public String markAsRead(Authentication auth, @PathVariable Long id) {
        AppUser me = userRepo.findByUsername(auth.getName()).orElseThrow();

        Notification n = notificationRepo.findById(id).orElseThrow();

        // Safety: only the owner can change their notification
        if (!n.getRecipient().getId().equals(me.getId())) {
            throw new SecurityException("Cannot modify another user's notifications.");
        }

        if (!n.isRead()) {
            n.setRead(true);
            notificationRepo.save(n);
        }

        return "redirect:/notifications";
    }
}
