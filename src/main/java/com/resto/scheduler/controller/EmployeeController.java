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

@Controller
@RequestMapping("/employee")
public class EmployeeController {

  private final AppUserRepository userRepo;
  private final AvailabilityRepository availabilityRepo;
  private final AssignmentRepository assignmentRepo;

  public EmployeeController(AppUserRepository userRepo,
                            AvailabilityRepository availabilityRepo,
                            AssignmentRepository assignmentRepo) {
    this.userRepo = userRepo;
    this.availabilityRepo = availabilityRepo;
    this.assignmentRepo = assignmentRepo;
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

  // === My Schedule (two weeks: current Mon–Sun + next Mon–Sun) ===
  @GetMapping("/schedule")
  public String mySchedule(Authentication auth, Model model) {
    model.addAttribute("active", "employee-schedule");

    // logged-in user (employees and managers both land here)
    AppUser me = userRepo.findByUsername(auth.getName())
            .orElseThrow(() -> new IllegalStateException("User not found: " + auth.getName()));

    // Two full weeks: current Mon–Sun + next Mon–Sun
    LocalDate today = LocalDate.now();
    LocalDate week1Start = today.with(DayOfWeek.MONDAY);
    LocalDate week2Start = week1Start.plusWeeks(1);
    LocalDate endInclusive = week2Start.plusDays(6);

    // Load ALL assignments for the grid (not just "mine")
    var allAssignments = assignmentRepo.findByShift_DateBetween(week1Start, endInclusive);

    // Build nested map: date -> roleKey -> assignment
    // roleKey = PERIOD + "_" + POSITION enum (e.g. "DINNER_SERVER_1", "LUNCH_LUNCH_SERVER")
    Map<String, Map<String, Assignment>> assignmentsGrid = new HashMap<>();
    for (Assignment a : allAssignments) {
      var s = a.getShift();
      String dateKey = s.getDate().toString(); // yyyy-MM-dd
      String roleKey = s.getPeriod().name() + "_" + s.getPosition().name();

      assignmentsGrid
              .computeIfAbsent(dateKey, k -> new HashMap<>())
              .put(roleKey, a);
    }

    // Build day lists for two weeks
    List<LocalDate> week1 = new ArrayList<>();
    List<LocalDate> week2 = new ArrayList<>();
    for (int i = 0; i < 7; i++) week1.add(week1Start.plusDays(i));
    for (int i = 0; i < 7; i++) week2.add(week2Start.plusDays(i));

    model.addAttribute("startDate", week1Start);
    model.addAttribute("week1", week1);
    model.addAttribute("week2", week2);

    // Provide the grid map used by the fragment
    model.addAttribute("assignmentsGrid", assignmentsGrid);

    return "employee/schedule";
  }
}
