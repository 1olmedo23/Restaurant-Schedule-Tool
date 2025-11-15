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
@RequestMapping("/manager/requests")
public class ManagerRequestController {

    private final RequestService requestService;
    private final AppUserRepository userRepo;
    private final RequestRepository requestRepo;

    public ManagerRequestController(RequestService requestService,
                                    AppUserRepository userRepo,
                                    RequestRepository requestRepo) {
        this.requestService = requestService;
        this.userRepo = userRepo;
        this.requestRepo = requestRepo;
    }

    /** Managers can also create requests for themselves (same UI as employee). */
    @GetMapping
    public String requestsHome(Authentication auth, Model model) {
        AppUser me = me(auth);
        model.addAttribute("me", me);
        model.addAttribute("requests", requestService.listForUser(me));
        model.addAttribute("employees", userRepo.findAll());
        return "manager/requests";
    }

    @PostMapping("/time-off")
    public String submitTimeOff(Authentication auth,
                                @RequestParam("date") @NotBlank String dateStr) {
        AppUser me = me(auth);
        LocalDate date = LocalDate.parse(dateStr);

        // Manager requesting for THEMSELVES, no receiver supplied here
        // This will behave like the employee flow:
        // - Not scheduled that day → TIME_OFF request
        // - Scheduled that day → throws IllegalArgumentException (must trade)
        requestService.createTimeOffOrTradeAuto(me, date, null);

        return "redirect:/manager/requests?submitted";
    }

    @PostMapping("/trade")
    public String submitTrade(Authentication auth,
                              @RequestParam("date") String dateStr,
                              @RequestParam("receiverId") Long receiverId) {
        AppUser me = me(auth);
        LocalDate date = LocalDate.parse(dateStr);

        AppUser receiver = userRepo.findById(receiverId).orElseThrow();

        // RequestService.createTrade now expects a String username as 3rd param
        requestService.createTrade(me, date, receiver.getUsername());

        return "redirect:/manager/requests?trade_submitted";
    }

    /** Process queue (approve/deny) */
    @GetMapping("/process")
    public String processQueue(@RequestParam(value = "start", required = false) String startStr,
                               @RequestParam(value = "end", required = false) String endStr,
                               Model model) {
        LocalDate start = (startStr != null ? LocalDate.parse(startStr) : LocalDate.now().minusYears(1));
        LocalDate end   = (endStr != null ? LocalDate.parse(endStr) : LocalDate.now().plusYears(1));
        List<Request> pending = requestService.listPending(start, end);
        model.addAttribute("pending", pending);
        model.addAttribute("start", start);
        model.addAttribute("end", end);
        return "manager/process";
    }

    @PostMapping("/{id}/approve")
    public String approve(Authentication auth, @PathVariable Long id,
                          @RequestParam(value = "note", required = false) String note,
                          @RequestParam(value = "override", required = false, defaultValue = "false") boolean override,
                          @RequestParam(value = "redirect", required = false, defaultValue = "process") String redirect) {
        AppUser mgr = me(auth);
        Request r = requestRepo.findById(id).orElseThrow();

        if (r.getType() == com.resto.scheduler.model.enums.RequestType.TRADE
                && !r.isReceiverConfirmed()
                && !override) {
            return "redirect:/manager/requests/process?error=receiver_not_confirmed";
        }

        requestService.approve(r, mgr, note);
        return "redirect:/manager/requests/" + redirect + "?approved";
    }

    @PostMapping("/{id}/deny")
    public String deny(Authentication auth, @PathVariable Long id,
                       @RequestParam(value = "note", required = false) String note,
                       @RequestParam(value = "redirect", required = false, defaultValue = "process") String redirect) {
        AppUser mgr = me(auth);
        Request r = requestRepo.findById(id).orElseThrow();
        requestService.deny(r, mgr, note);
        return "redirect:/manager/requests/" + redirect + "?denied";
    }

    private AppUser me(Authentication auth) {
        return userRepo.findByUsername(auth.getName()).orElseThrow();
    }
}
