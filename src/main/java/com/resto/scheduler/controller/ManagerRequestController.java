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
import com.resto.scheduler.model.enums.ShiftPeriod;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.time.Instant;

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
    public String requestsHome() {
        // Managers should use the shared Requests page
        return "redirect:/employee/requests";
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
                              @RequestParam("period") String periodStr,
                              @RequestParam("receiverId") Long receiverId) {
        AppUser me = me(auth);
        LocalDate date = LocalDate.parse(dateStr);

        AppUser receiver = userRepo.findById(receiverId).orElseThrow();

        try {
            ShiftPeriod period = ShiftPeriod.valueOf(periodStr);
            requestService.createTrade(me, date, period, receiver.getUsername());
            return "redirect:/manager/requests?trade_submitted";
        } catch (IllegalArgumentException ex) {
            String msg = URLEncoder.encode(ex.getMessage(), StandardCharsets.UTF_8);
            return "redirect:/manager/requests?error=" + msg;
        }
    }

    /** Process queue (approve/deny) */
    @GetMapping("/process")
    public String processQueue(@RequestParam(value = "start", required = false) String startStr,
                               @RequestParam(value = "end", required = false) String endStr,
                               Model model) {
        LocalDate start = (startStr != null ? LocalDate.parse(startStr) : LocalDate.now());
        LocalDate end   = (endStr != null ? LocalDate.parse(endStr) : LocalDate.now().plusYears(1));

        // Pending (oldest first)
        List<Request> pending = requestService.listPending(start, end);

        // History (APPROVED + DENIED, newest decisions first)
        List<Request> decided = requestService.listDecided(start, end);

        // Decide which rows need seconds shown (collisions in the same minute)
        Map<Long, Boolean> pendingShowSeconds = buildShowSecondsMap(pending);
        Map<Long, Boolean> decidedShowSeconds = buildShowSecondsMap(decided);

        model.addAttribute("pending", pending);
        model.addAttribute("decided", decided);
        model.addAttribute("pendingShowSeconds", pendingShowSeconds);
        model.addAttribute("decidedShowSeconds", decidedShowSeconds);
        model.addAttribute("start", start);
        model.addAttribute("end", end);
        model.addAttribute("active", "manager-process-requests");

        return "manager/process";
    }

    @PostMapping("/{id}/approve")
    public String approve(Authentication auth,
                          @PathVariable Long id,
                          @RequestParam(value = "note", required = false) String note,
                          @RequestParam(value = "override", required = false) String overrideParam,
                          @RequestParam(value = "redirect", required = false, defaultValue = "process") String redirect) {

        AppUser mgr = me(auth);
        Request r = requestRepo.findById(id).orElseThrow();

        // Any presence of "override" counts as true
        boolean override = (overrideParam != null);

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

    /**
     * For a given list of requests, mark which ones share a createdAt minute
     * with at least one other request. Those will show seconds in the UI.
     */
    private Map<Long, Boolean> buildShowSecondsMap(List<Request> requests) {
        Map<String, Integer> minuteCounts = new HashMap<>();

        // First pass: count how many requests fall into each minute
        for (Request r : requests) {
            Instant created = r.getCreatedAt();
            if (created == null) continue;

            Instant minuteKey = created.truncatedTo(ChronoUnit.MINUTES);
            String key = minuteKey.toString();
            minuteCounts.merge(key, 1, Integer::sum);
        }

        // Second pass: mark requests that share their minute with others
        Map<Long, Boolean> result = new HashMap<>();
        for (Request r : requests) {
            Instant created = r.getCreatedAt();
            if (created == null || r.getId() == null) {
                result.put(r.getId(), false);
                continue;
            }
            Instant minuteKey = created.truncatedTo(ChronoUnit.MINUTES);
            String key = minuteKey.toString();
            boolean showSeconds = minuteCounts.getOrDefault(key, 0) > 1;
            result.put(r.getId(), showSeconds);
        }

        return result;
    }
}
