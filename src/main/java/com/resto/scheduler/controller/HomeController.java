package com.resto.scheduler.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

  @GetMapping("/")
  public String root(Authentication auth) {
    // If NOT logged in -> show landing page
    if (auth == null || !auth.isAuthenticated()) {
      return "index"; // src/main/resources/templates/index.html
    }

    // Logged in -> redirect based on role
    boolean isManager = auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_MANAGER".equals(a.getAuthority()));

    if (isManager) {
      return "redirect:/manager/schedule-builder";
    } else {
      return "redirect:/employee/schedule";
    }
  }

  @GetMapping("/login")
  public String login(Authentication auth) {
    // If already logged in, do NOT show login again
    if (auth != null && auth.isAuthenticated()) {
      boolean isManager = auth.getAuthorities().stream()
              .anyMatch(a -> "ROLE_MANAGER".equals(a.getAuthority()));

      if (isManager) {
        return "redirect:/manager/schedule-builder";
      } else {
        return "redirect:/employee/schedule";
      }
    }

    // Anonymous user -> show login page
    return "login";
  }

  @GetMapping("/post-login")
  public String postLogin(Authentication auth) {
    if (auth != null && auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"))) {
      return "redirect:/manager/schedule-builder";
    }
    return "redirect:/employee/schedule";
  }
}
