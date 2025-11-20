package com.resto.scheduler.controller;

import com.resto.scheduler.model.AppUser;
import com.resto.scheduler.repository.AppUserRepository;
import com.resto.scheduler.repository.NotificationRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice
public class GlobalModelAttributes {

    private final AppUserRepository userRepo;
    private final NotificationRepository notificationRepo;

    public GlobalModelAttributes(AppUserRepository userRepo,
                                 NotificationRepository notificationRepo) {
        this.userRepo = userRepo;
        this.notificationRepo = notificationRepo;
    }

    /**
     * Adds a global notificationUnreadCount attribute for all views.
     * If the user is not logged in, the attribute may be absent or 0.
     */
    @ModelAttribute
    public void addGlobalAttributes(Model model, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return;
        }

        AppUser me = userRepo.findByUsername(auth.getName()).orElse(null);
        if (me == null) {
            return;
        }

        long unreadCount = notificationRepo.countByRecipientAndReadIsFalse(me);
        model.addAttribute("notificationUnreadCount", unreadCount);
    }
}
