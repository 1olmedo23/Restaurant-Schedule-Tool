package com.resto.scheduler.controller;

import com.resto.scheduler.model.Assignment;
import com.resto.scheduler.model.SchedulePeriod;
import com.resto.scheduler.repository.AssignmentRepository;
import com.resto.scheduler.repository.PublishedAssignmentRepository;
import com.resto.scheduler.repository.SchedulePeriodRepository;
import com.resto.scheduler.service.PublishService;
import com.resto.scheduler.service.ScheduleViewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/manager/publish")
public class ManagerPublishController {

    private final SchedulePeriodRepository schedulePeriods;
    private final AssignmentRepository assignmentRepo;
    private final ScheduleViewService scheduleView;
    private final PublishService publishService;
    private final PublishedAssignmentRepository publishedAssignmentRepository;

    public ManagerPublishController(SchedulePeriodRepository schedulePeriods,
                                    AssignmentRepository assignmentRepo,
                                    ScheduleViewService scheduleView,
                                    PublishService publishService,
                                    PublishedAssignmentRepository publishedAssignmentRepository) {
        this.schedulePeriods = schedulePeriods;
        this.assignmentRepo = assignmentRepo;
        this.scheduleView = scheduleView;
        this.publishService = publishService;
        this.publishedAssignmentRepository = publishedAssignmentRepository;
    }

    /** Create-or-get, used only by POST actions (not by GET/browse). */
    private SchedulePeriod ensurePeriodForStart(LocalDate start) {
        LocalDate base = scheduleView.mondayOf(start);
        return schedulePeriods.findByStartDate(base).orElseGet(() -> {
            SchedulePeriod sp = new SchedulePeriod();
            sp.setStartDate(base);
            sp.setEndDate(base.plusDays(13));
            sp.setStatus("DRAFT");
            return schedulePeriods.save(sp);
        });
    }

    /** GET: browse any 2-week window without creating a period. */
    @GetMapping
    public String publishPage(
            @RequestParam(value = "start", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            LocalDate start,
            Model model
    ) {
        // 1) Anchor date: either requested start (for navigation) or today
        LocalDate anchor = (start != null) ? start : LocalDate.now();

        // 2) Try to find a POSTED period that CONTAINS the anchor date
        LocalDate baseStart;
        var spOpt = schedulePeriods.findPostedContaining(anchor);

        if (spOpt.isPresent()) {
            // Use the real posted period boundaries
            SchedulePeriod sp = spOpt.get();
            baseStart = sp.getStartDate();
        } else {
            // No posted period contains anchor → fall back to Monday-of-anchor
            baseStart = scheduleView.mondayOf(anchor);
            spOpt = schedulePeriods.findByStartDate(baseStart);
        }

        LocalDate endInclusive = baseStart.plusDays(13);

        // Week lists
        List<LocalDate> week1 = new ArrayList<>(7);
        List<LocalDate> week2 = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) week1.add(baseStart.plusDays(i));
        for (int i = 0; i < 7; i++) week2.add(baseStart.plusDays(7 + i));

        // Live assignments for this window (preview)
        var allAssignments = assignmentRepo.findByShift_DateBetween(baseStart, endInclusive);
        Map<String, Map<String, Assignment>> assignmentsGrid = new HashMap<>();
        for (var a : allAssignments) {
            var s = a.getShift();
            if (s == null) continue;
            String dateKey = s.getDate().toString();
            String roleKey = s.getPeriod().name() + "_" + s.getPosition().name();
            assignmentsGrid.computeIfAbsent(dateKey, k -> new HashMap<>()).put(roleKey, a);
        }

        boolean hasPeriod = spOpt.isPresent();
        boolean isPosted  = hasPeriod && "POSTED".equalsIgnoreCase(spOpt.get().getStatus());

        // Diff-based needsRepublish (snapshot vs live), only if POSTED
        boolean needsRepublish = false;
        if (isPosted) {
            Long spId = spOpt.get().getId();
            var snapshotRows = publishedAssignmentRepository.findBySchedulePeriod_Id(spId);

            Map<String, Map<String, Long>> snapGrid = new HashMap<>();
            for (var pr : snapshotRows) {
                String dk = pr.getDate().toString();
                String rk = pr.getPeriod().name() + "_" + pr.getPosition().name();
                snapGrid.computeIfAbsent(dk, k -> new HashMap<>())
                        .put(rk, pr.getUser() != null ? pr.getUser().getId() : null);
            }

            // 1) live vs snapshot mismatches
            outer:
            for (var eDay : assignmentsGrid.entrySet()) {
                var liveRoles = eDay.getValue();
                var snapRoles = snapGrid.getOrDefault(eDay.getKey(), Collections.emptyMap());
                for (var eRole : liveRoles.entrySet()) {
                    Long liveUid = (eRole.getValue().getEmployee() != null)
                            ? eRole.getValue().getEmployee().getId() : null;
                    Long snapUid = snapRoles.getOrDefault(eRole.getKey(), null);
                    if (!Objects.equals(liveUid, snapUid)) { needsRepublish = true; break outer; }
                }
            }
            // 2) roles present in snapshot but removed in live
            if (!needsRepublish) {
                outer2:
                for (var eDay : snapGrid.entrySet()) {
                    var snapRoles = eDay.getValue();
                    var liveRoles = assignmentsGrid.getOrDefault(eDay.getKey(), Collections.emptyMap());
                    for (String rk : snapRoles.keySet()) {
                        if (!liveRoles.containsKey(rk)) { needsRepublish = true; break outer2; }
                    }
                }
            }
        }

        DateTimeFormatter bannerFormatter = DateTimeFormatter.ofPattern("MM-dd-yy");

        String banner;
        if (isPosted && spOpt.isPresent()) {
            LocalDate s = spOpt.get().getStartDate();
            LocalDate e = spOpt.get().getEndDate();
            String range = s.format(bannerFormatter) + " to " + e.format(bannerFormatter);
            banner = "2-week period: " + range + " — Status: POSTED";
        } else {
            String range = baseStart.format(bannerFormatter) + " to " + endInclusive.format(bannerFormatter);
            banner = "2-week period: " + range + " — Status: DRAFT";
        }

        model.addAttribute("banner", banner);
        model.addAttribute("period", spOpt.orElse(null));
        model.addAttribute("hasPeriod", hasPeriod);
        model.addAttribute("isPosted", isPosted);
        model.addAttribute("needsRepublish", needsRepublish);

        model.addAttribute("week1", week1);
        model.addAttribute("week2", week2);
        model.addAttribute("assignmentsGrid", assignmentsGrid);

        model.addAttribute("prevStart", baseStart.minusDays(14));
        model.addAttribute("nextStart", baseStart.plusDays(14));
        model.addAttribute("windowStart", baseStart);
        model.addAttribute("active", "manager-publish");
        return "manager/publish";
    }

    /** POST: Post the visible window (create the period if missing), snapshot it. */
    @PostMapping("/post")
    public String postSchedule(
            @RequestParam("start")
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            LocalDate start,
            HttpServletRequest request
    ) {
        SchedulePeriod sp = ensurePeriodForStart(start);
        sp.setStatus("POSTED");
        sp.setPostedAt(OffsetDateTime.now(ZoneOffset.UTC));

        Object uid = request.getSession().getAttribute("userId");
        if (uid instanceof Long l) sp.setPostedByUserId(l);

        schedulePeriods.save(sp);
        publishService.snapshotPeriod(sp.getId());
        return "redirect:/manager/publish?start=" + sp.getStartDate() + "&posted=1";
    }

    /** POST: Republish the visible window (only if already POSTED). */
    @PostMapping("/republish")
    public String republish(
            @RequestParam("start")
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            LocalDate start
    ) {
        LocalDate base = scheduleView.mondayOf(start);
        var spOpt = schedulePeriods.findByStartDate(base);
        if (spOpt.isPresent() && "POSTED".equalsIgnoreCase(spOpt.get().getStatus())) {
            publishService.snapshotPeriod(spOpt.get().getId());
            return "redirect:/manager/publish?start=" + base + "&republished=1";
        }
        return "redirect:/manager/publish?start=" + base + "&republished=0";
    }
}
