package com.resto.scheduler.controller;

import com.resto.scheduler.model.AppUser;
import com.resto.scheduler.model.Request;
import com.resto.scheduler.repository.AppUserRepository;
import com.resto.scheduler.repository.RequestRepository;
import com.resto.scheduler.service.RequestService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/employee/requests")
public class EmployeeRequestController {

    private final RequestService requestService;
    private final AppUserRepository userRepo;
    private final RequestRepository requestRepo;

    public EmployeeRequestController(RequestService requestService,
                                     AppUserRepository userRepo,
                                     RequestRepository requestRepo) {
        this.requestService = requestService;
        this.userRepo = userRepo;
        this.requestRepo = requestRepo;
    }

    @GetMapping
    public String requestsHome(Authentication auth, Model model) {
        AppUser me = me(auth);
        model.addAttribute("me", me);
        model.addAttribute("requests", requestService.listForUser(me));               // my outbound
        model.addAttribute("inbound", requestRepo.findByReceiverOrderByCreatedAtDesc(me)); // to-me
        model.addAttribute("employees", userRepo.findAll());
        return "employee/requests";
    }

    @PostMapping("/time-off")
    public String submitTimeOff(Authentication auth,
                                @RequestParam("date") @NotBlank String dateStr,
                                Model model) {
        AppUser me = me(auth);
        LocalDate date = LocalDate.parse(dateStr);
        try {
            // Auto: if not scheduled → TIME_OFF; if scheduled → throws IllegalArgumentException
            requestService.createTimeOffOrTradeAuto(me, date, null);
            return "redirect:/employee/requests?submitted";
        } catch (IllegalArgumentException ex) {
            // Show a friendly banner telling them to use Trade with a receiver
            return "redirect:/employee/requests?error="
                    + java.net.URLEncoder.encode(ex.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @PostMapping("/trade")
    public String submitTrade(Authentication auth,
                              @RequestParam("date") String dateStr,
                              @RequestParam("receiverId") Long receiverId) {
        AppUser me = me(auth);
        LocalDate date = LocalDate.parse(dateStr);
        AppUser receiver = userRepo.findById(receiverId).orElseThrow();

        // RequestService.createTrade now expects receiverUsername (String), not AppUser
        requestService.createTrade(me, date, receiver.getUsername());

        return "redirect:/employee/requests?trade_submitted";
    }

    @PostMapping("/{id}/confirm")
    public String confirmTrade(Authentication auth, @PathVariable Long id) {
        AppUser me = me(auth);
        Request r = requestRepo.findById(id).orElseThrow();
        requestService.receiverConfirm(r, me);
        return "redirect:/employee/requests?confirmed";
    }

    private AppUser me(Authentication auth) {
        return userRepo.findByUsername(auth.getName()).orElseThrow();
    }
}
