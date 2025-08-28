package com.resto.scheduler.controller;

import com.resto.scheduler.model.Assignment;
import com.resto.scheduler.model.SchedulePeriod;
import com.resto.scheduler.repository.AssignmentRepository;
import com.resto.scheduler.repository.SchedulePeriodRepository;
import com.resto.scheduler.service.ScheduleViewService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.*;
import java.util.*;

@Controller
@RequestMapping("/manager/publish")
public class ManagerPublishController {

    private final SchedulePeriodRepository schedulePeriods;
    private final AssignmentRepository assignmentRepo;
    private final ScheduleViewService scheduleView;

    public ManagerPublishController(SchedulePeriodRepository schedulePeriods,
                                    AssignmentRepository assignmentRepo,
                                    ScheduleViewService scheduleView) {
        this.schedulePeriods = schedulePeriods;
        this.assignmentRepo = assignmentRepo;
        this.scheduleView = scheduleView;
    }

    private SchedulePeriod ensureCurrentPeriod() {
        LocalDate start = scheduleView.mondayOf(LocalDate.now(ZoneId.systemDefault()));
        return schedulePeriods.findByStartDate(start).orElseGet(() -> {
            SchedulePeriod sp = new SchedulePeriod();
            sp.setStartDate(start);
            sp.setEndDate(start.plusDays(13));
            sp.setStatus("DRAFT");
            return schedulePeriods.save(sp);
        });
    }

    @GetMapping
    public String publishPage(Model model) {
        SchedulePeriod sp = ensureCurrentPeriod();
        model.addAttribute("period", sp);
        model.addAttribute("banner",
                "Current 2-week period: " + sp.getStartDate() + " to " + sp.getEndDate() +
                        " — Status: " + sp.getStatus());

        LocalDate periodStart = sp.getStartDate();
        LocalDate week2Start  = periodStart.plusWeeks(1);
        LocalDate endInclusive = week2Start.plusDays(6);

        List<LocalDate> week1 = new ArrayList<>(7);
        List<LocalDate> week2 = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) week1.add(periodStart.plusDays(i));
        for (int i = 0; i < 7; i++) week2.add(week2Start.plusDays(i));

        List<Assignment> allAssignments =
                assignmentRepo.findByShift_DateBetween(periodStart, endInclusive);

        Map<String, Map<String, Assignment>> assignmentsGrid = new HashMap<>();
        for (Assignment a : allAssignments) {
            var s = a.getShift();
            if (s == null) continue;
            String dateKey = s.getDate().toString(); // yyyy-MM-dd
            String roleKey = s.getPeriod().name() + "_" + s.getPosition().name();
            assignmentsGrid.computeIfAbsent(dateKey, k -> new HashMap<>()).put(roleKey, a);
        }

        model.addAttribute("week1", week1);
        model.addAttribute("week2", week2);
        model.addAttribute("assignmentsGrid", assignmentsGrid);
        model.addAttribute("active", "manager-publish");

        return "manager/publish";
    }

    @PostMapping
    public String postSchedule(Authentication auth, HttpServletRequest request) {
        SchedulePeriod sp = ensureCurrentPeriod();
        sp.setStatus("POSTED");
        sp.setPostedAt(OffsetDateTime.now(ZoneOffset.UTC));

        Object uid = request.getSession().getAttribute("userId");
        if (uid instanceof Long) sp.setPostedByUserId((Long) uid);

        schedulePeriods.save(sp);
        return "redirect:/manager/publish?posted=1";
    }
}
