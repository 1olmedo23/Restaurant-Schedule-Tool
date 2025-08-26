package com.resto.scheduler.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

  @GetMapping("/")
  public String index() { return "index"; }

  @GetMapping("/login")
  public String login() { return "login"; }

  @GetMapping("/post-login")
  public String postLogin(Authentication auth) {
    if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"))) {
      return "redirect:/manager/schedule-builder";
    }
    return "redirect:/employee/schedule";
  }
}
