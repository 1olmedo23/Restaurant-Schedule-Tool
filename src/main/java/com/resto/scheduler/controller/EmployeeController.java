package com.resto.scheduler.controller;

import com.resto.scheduler.model.AppUser;
import com.resto.scheduler.model.Assignment;
import com.resto.scheduler.model.Availability;
import com.resto.scheduler.repository.AppUserRepository;
import com.resto.scheduler.repository.AssignmentRepository;
import com.resto.scheduler.repository.AvailabilityRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import com.resto.scheduler.service.ScheduleViewService;

@Controller
@RequestMapping("/employee")
public class EmployeeController {

  private final AppUserRepository userRepo;
  private final AvailabilityRepository availabilityRepo;
  private final AssignmentRepository assignmentRepo;
  private final ScheduleViewService scheduleViewService;

  public EmployeeController(AppUserRepository userRepo,
                            AvailabilityRepository availabilityRepo,
                            AssignmentRepository assignmentRepo,
                            ScheduleViewService scheduleViewService) {
    this.userRepo = userRepo;
    this.availabilityRepo = availabilityRepo;
    this.assignmentRepo = assignmentRepo;
    this.scheduleViewService = scheduleViewService;
  }

  // === Availability (Tue–Sat) ===
  @GetMapping("/availability")
  public String availabilityForm(Model model, Principal principal) {
    List<DayOfWeek> weekdays = List.of(
            DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
    );

    AppUser user = userRepo.findByUsername(principal.getName()).orElseThrow();

    Map<String, Map<String, Boolean>> availMap = new HashMap<>();
    availabilityRepo.findByUser(user).forEach(a -> {
      Map<String,Boolean> m = new HashMap<>();
      m.put("lunch", a.isLunchAvailable());
      m.put("dinner", a.isDinnerAvailable());
      availMap.put(a.getDayOfWeek().name(), m);
    });

    model.addAttribute("active", "employee-availability");
    model.addAttribute("weekdays", weekdays);
    model.addAttribute("avail", availMap);
    return "employee/availability";
  }

  @PostMapping("/availability")
  public String saveAvailability(@RequestParam Map<String, String> params, Principal principal) {
    AppUser user = userRepo.findByUsername(principal.getName()).orElseThrow();

    for (DayOfWeek d : List.of(
            DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)) {

      boolean lunch  = params.containsKey("lunch_"  + d.name());
      boolean dinner = params.containsKey("dinner_" + d.name());

      Availability a = availabilityRepo.findByUserAndDayOfWeek(user, d).orElseGet(Availability::new);
      a.setUser(user);
      a.setDayOfWeek(d);
      a.setLunchAvailable(lunch);
      a.setDinnerAvailable(dinner);
      availabilityRepo.save(a);
    }
    return "redirect:/employee/availability?saved";
  }

  // === My Schedule: navigate every 2-week block; populate only if POSTED ===
  @GetMapping("/schedule")
  public String mySchedule(
          @RequestParam(value = "start", required = false)
          @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
          java.time.LocalDate start,
          org.springframework.ui.Model model) {

    model.addAttribute("active", "employee-schedule");

    // Determine target 2-week block start (aligned to Monday)
    java.time.LocalDate baseStart;
    if (start != null) {
      baseStart = scheduleViewService.mondayOf(start);
    } else {
      // default: latest POSTED start, else current Monday
      baseStart = scheduleViewService.latestPostedStart()
              .orElse(scheduleViewService.mondayOf(java.time.LocalDate.now()));
    }

    java.time.LocalDate periodStart  = baseStart;
    java.time.LocalDate week2Start   = periodStart.plusWeeks(1);
    java.time.LocalDate endInclusive = week2Start.plusDays(6);

    // Build the two week date lists (always render the grid)
    java.util.List<java.time.LocalDate> week1 = new java.util.ArrayList<>(7);
    java.util.List<java.time.LocalDate> week2 = new java.util.ArrayList<>(7);
    for (int i = 0; i < 7; i++) week1.add(periodStart.plusDays(i));
    for (int i = 0; i < 7; i++) week2.add(week2Start.plusDays(i));

    // Populate assignments ONLY if this start corresponds to a POSTED period
    boolean isPosted = scheduleViewService.isPostedStart(periodStart);
    boolean anyPosted = scheduleViewService.latestPostedStart().isPresent();

    java.util.Map<String, java.util.Map<String, com.resto.scheduler.model.Assignment>> assignmentsGrid = new java.util.HashMap<>();
    if (isPosted) {
      var allAssignments = assignmentRepo.findByShift_DateBetween(periodStart, endInclusive);
      for (com.resto.scheduler.model.Assignment a : allAssignments) {
        var s = a.getShift();
        if (s == null) continue;
        String dateKey = s.getDate().toString();
        String roleKey = s.getPeriod().name() + "_" + s.getPosition().name();
        assignmentsGrid.computeIfAbsent(dateKey, k -> new java.util.HashMap<>()).put(roleKey, a);
      }
    }

    // Prev/Next are always available (step by 2 weeks)
    java.time.LocalDate prevStart = periodStart.minusWeeks(2);
    java.time.LocalDate nextStart = periodStart.plusWeeks(2);

    model.addAttribute("hasPeriods", true); // we always render the grid now
    model.addAttribute("isPosted", isPosted);
    model.addAttribute("startDate", periodStart);
    model.addAttribute("endDate", periodStart.plusDays(13));
    model.addAttribute("week1", week1);
    model.addAttribute("week2", week2);
    model.addAttribute("assignmentsGrid", assignmentsGrid);
    model.addAttribute("prevStart", prevStart);
    model.addAttribute("nextStart", nextStart);
    model.addAttribute("anyPosted", anyPosted);

    return "employee/schedule";
  }
}
