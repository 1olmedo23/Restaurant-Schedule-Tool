package com.resto.scheduler.controller;

import com.resto.scheduler.model.AppUser;
import com.resto.scheduler.model.Assignment;
import com.resto.scheduler.model.Availability;
import com.resto.scheduler.model.PublishedAssignment;
import com.resto.scheduler.model.SchedulePeriod;
import com.resto.scheduler.model.Shift;
import com.resto.scheduler.repository.AppUserRepository;
import com.resto.scheduler.repository.AssignmentRepository;
import com.resto.scheduler.repository.AvailabilityRepository;
import com.resto.scheduler.repository.PublishedAssignmentRepository;
import com.resto.scheduler.repository.SchedulePeriodRepository;
import com.resto.scheduler.service.ScheduleViewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/employee")
public class EmployeeController {

  private final AppUserRepository userRepo;
  private final AvailabilityRepository availabilityRepo;
  private final AssignmentRepository assignmentRepo; // kept (used elsewhere)
  private final ScheduleViewService scheduleViewService;
  private final PublishedAssignmentRepository publishedAssignmentRepo;
  private final SchedulePeriodRepository schedulePeriodRepo;

  public EmployeeController(AppUserRepository userRepo,
                            AvailabilityRepository availabilityRepo,
                            AssignmentRepository assignmentRepo,
                            ScheduleViewService scheduleViewService,
                            PublishedAssignmentRepository publishedAssignmentRepo,
                            SchedulePeriodRepository schedulePeriodRepo) {
    this.userRepo = userRepo;
    this.availabilityRepo = availabilityRepo;
    this.assignmentRepo = assignmentRepo;
    this.scheduleViewService = scheduleViewService;
    this.publishedAssignmentRepo = publishedAssignmentRepo;
    this.schedulePeriodRepo = schedulePeriodRepo;
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

  // === My Schedule: navigate every 2-week block; populate only if POSTED (from published snapshot) ===
  @GetMapping("/schedule")
  public String mySchedule(
          @RequestParam(value = "start", required = false)
          @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
          LocalDate start,
          Model model
  ) {
    model.addAttribute("active", "employee-schedule");

    // Determine target 2-week block (Monday anchor)
    LocalDate baseStart = (start != null)
            ? scheduleViewService.mondayOf(start)
            : scheduleViewService.latestPostedStart().orElse(scheduleViewService.mondayOf(LocalDate.now()));

    LocalDate periodStart  = baseStart;
    LocalDate week2Start   = periodStart.plusWeeks(1);
    LocalDate endInclusive = week2Start.plusDays(6);

    // Build date lists (always render the grid)
    List<LocalDate> week1 = new ArrayList<>(7);
    List<LocalDate> week2 = new ArrayList<>(7);
    for (int i = 0; i < 7; i++) week1.add(periodStart.plusDays(i));
    for (int i = 0; i < 7; i++) week2.add(week2Start.plusDays(i));

    // Is THIS window a POSTED period?
    boolean isPosted = schedulePeriodRepo.findByStartDate(periodStart)
            .map(sp -> "POSTED".equalsIgnoreCase(sp.getStatus()))
            .orElse(false);

    boolean anyPosted = scheduleViewService.latestPostedStart().isPresent();

    // Build assignmentsGrid for the fragment.
    Map<String, Map<String, Assignment>> assignmentsGrid = new HashMap<>();

    if (isPosted) {
      // Use the published snapshot for the entire 2-week window
      List<PublishedAssignment> rows = publishedAssignmentRepo.findByDateBetween(periodStart, endInclusive);

      for (PublishedAssignment pa : rows) {
        // Create transient Shift + Assignment so fragments render unchanged
        Shift s = new Shift();
        s.setDate(pa.getDate());
        s.setPeriod(pa.getPeriod());
        s.setPosition(pa.getPosition());

        Assignment a = new Assignment();
        a.setShift(s);
        a.setEmployee(pa.getUser()); // can be null

        String dateKey = pa.getDate().toString(); // yyyy-MM-dd
        String roleKey = pa.getPeriod().name() + "_" + pa.getPosition().name();

        assignmentsGrid.computeIfAbsent(dateKey, k -> new HashMap<>()).put(roleKey, a);
      }
    }

    // Prev/Next: step 14 days
    LocalDate prevStart = periodStart.minusDays(14);
    LocalDate nextStart = periodStart.plusDays(14);

    model.addAttribute("hasPeriods", true);
    model.addAttribute("isPosted", isPosted);
    model.addAttribute("anyPosted", anyPosted);

    model.addAttribute("startDate", periodStart);
    model.addAttribute("endDate", periodStart.plusDays(13));

    model.addAttribute("week1", week1);
    model.addAttribute("week2", week2);
    model.addAttribute("assignmentsGrid", assignmentsGrid);

    model.addAttribute("prevStart", prevStart);
    model.addAttribute("nextStart", nextStart);

    return "employee/schedule";
  }
}
