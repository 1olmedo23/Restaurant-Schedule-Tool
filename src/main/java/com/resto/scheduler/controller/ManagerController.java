package com.resto.scheduler.controller;

import com.resto.scheduler.model.AppUser;
import com.resto.scheduler.model.Assignment;
import com.resto.scheduler.model.Shift;
import com.resto.scheduler.model.enums.Position;
import com.resto.scheduler.model.enums.ShiftPeriod;
import com.resto.scheduler.repository.AppUserRepository;
import com.resto.scheduler.repository.AssignmentRepository;
import com.resto.scheduler.repository.AvailabilityRepository;
import com.resto.scheduler.repository.SchedulePeriodRepository;
import com.resto.scheduler.repository.ShiftRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/manager")
public class ManagerController {

  public record RoleOption(String key, String label, ShiftPeriod period, Position position) {}

  private final AppUserRepository userRepo;
  private final ShiftRepository shiftRepo;
  private final AssignmentRepository assignmentRepo;
  private final AvailabilityRepository availabilityRepo;
  private final SchedulePeriodRepository schedulePeriodRepo;

  public ManagerController(AppUserRepository userRepo,
                           ShiftRepository shiftRepo,
                           AssignmentRepository assignmentRepo,
                           AvailabilityRepository availabilityRepo,
                           SchedulePeriodRepository schedulePeriodRepo) {
    this.userRepo = userRepo;
    this.shiftRepo = shiftRepo;
    this.assignmentRepo = assignmentRepo;
    this.availabilityRepo = availabilityRepo;
    this.schedulePeriodRepo = schedulePeriodRepo;
  }

  private boolean isLocked(LocalDate date) {
    return schedulePeriodRepo.findTopByStatusOrderByStartDateDesc("POSTED")
            .map(sp -> !date.isBefore(sp.getStartDate()) && !date.isAfter(sp.getEndDate()))
            .orElse(false);
  }

  // import stays: org.springframework.format.annotation.DateTimeFormat

  @GetMapping("/schedule-builder")
  public String scheduleBuilder(
          @RequestParam(value = "start", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
          Model model
  ) {
    // Anchor/start of the 14-day window: provided ?start=YYYY-MM-DD or today
    LocalDate windowStart = (start != null ? start : LocalDate.now());

    // Build 14 consecutive days (two weeks)
    List<LocalDate> days = new ArrayList<>(14);
    for (int i = 0; i < 14; i++) {
      days.add(windowStart.plusDays(i));
    }

    // Prev/next window anchors (jump exactly ±14 days)
    LocalDate prevStart = windowStart.minusDays(14);
    LocalDate nextStart = windowStart.plusDays(14);

    // collect all dates in this window that belong to any POSTED period
    LocalDate windowEnd = windowStart.plusDays(13);
    var postedPeriods = schedulePeriodRepo.findPostedOverlapping(windowStart, windowEnd);
    java.util.Set<LocalDate> postedDates = new java.util.HashSet<>();
    for (var p : postedPeriods) {
      for (LocalDate d = p.getStartDate(); !d.isAfter(p.getEndDate()); d = d.plusDays(1)) {
        if (!d.isBefore(windowStart) && !d.isAfter(windowEnd)) {
          postedDates.add(d);
        }
      }
    }

    model.addAttribute("days", days);
    model.addAttribute("windowStart", windowStart);
    model.addAttribute("windowEnd", windowStart.plusDays(13));
    model.addAttribute("prevStart", prevStart);
    model.addAttribute("nextStart", nextStart);

    // Keep Today + active for header and card tint
    model.addAttribute("today", LocalDate.now());
    model.addAttribute("postedDates", postedDates);

    model.addAttribute("active", "manager-schedule");
    return "manager/schedule-builder";
  }

  @GetMapping("/schedule/{date}")
  public String daySchedule(@PathVariable String date, Model model) {
    LocalDate target = LocalDate.parse(date, DateTimeFormatter.ISO_DATE);
    DayOfWeek dow = target.getDayOfWeek();

    List<RoleOption> lunchRoles = List.of(
            new RoleOption("role_LUNCH_SERVER","Server", ShiftPeriod.LUNCH, Position.LUNCH_SERVER),
            new RoleOption("role_LUNCH_ASSISTANT","Assistant", ShiftPeriod.LUNCH, Position.LUNCH_ASSISTANT),
            new RoleOption("role_LUNCH_MANAGER","Manager", ShiftPeriod.LUNCH, Position.LUNCH_MANAGER)
    );
    List<RoleOption> dinnerRoles = List.of(
            new RoleOption("role_DINNER_SERVER_1","Server 1", ShiftPeriod.DINNER, Position.SERVER_1),
            new RoleOption("role_DINNER_SERVER_2","Server 2", ShiftPeriod.DINNER, Position.SERVER_2),
            new RoleOption("role_DINNER_SERVER_3","Server 3", ShiftPeriod.DINNER, Position.SERVER_3),
            new RoleOption("role_DINNER_SUSHI","Sushi", ShiftPeriod.DINNER, Position.SUSHI),
            new RoleOption("role_DINNER_EXPO","Expo", ShiftPeriod.DINNER, Position.EXPO),
            new RoleOption("role_DINNER_BUSSER_1","Busser 1", ShiftPeriod.DINNER, Position.BUSSER_1),
            new RoleOption("role_DINNER_BUSSER_2","Busser 2", ShiftPeriod.DINNER, Position.BUSSER_2),
            new RoleOption("role_DINNER_HOST_1","Host 1", ShiftPeriod.DINNER, Position.HOST_1),
            new RoleOption("role_DINNER_HOST_2","Host 2", ShiftPeriod.DINNER, Position.HOST_2),
            new RoleOption("role_DINNER_FLOAT","FLOAT", ShiftPeriod.DINNER, Position.FLOAT)
    );

    // Staff lists
    List<AppUser> employees = userRepo.findByRoles_Name("EMPLOYEE");
    List<AppUser> managers  = userRepo.findByRoles_Name("MANAGER");

    List<AppUser> allStaff  = new ArrayList<>();
    allStaff.addAll(employees);
    allStaff.addAll(managers);
    allStaff.sort(Comparator.comparing(AppUser::getFullName));

    // Availability-aware lists
    List<AppUser> availLunchEmployees = filterByAvailability(employees, dow, ShiftPeriod.LUNCH);
    List<AppUser> availLunchManagers  = filterByAvailability(managers,  dow, ShiftPeriod.LUNCH);
    List<AppUser> availDinnerEmployees= filterByAvailability(employees, dow, ShiftPeriod.DINNER);
    List<AppUser> availDinnerManagers = filterByAvailability(managers,  dow, ShiftPeriod.DINNER);

    List<AppUser> availableLunchStaff = new ArrayList<>();
    availableLunchStaff.addAll(availLunchEmployees);
    availableLunchStaff.addAll(availLunchManagers);
    availableLunchStaff.sort(Comparator.comparing(AppUser::getFullName));

    List<AppUser> availableDinnerStaff = new ArrayList<>();
    availableDinnerStaff.addAll(availDinnerEmployees);
    availableDinnerStaff.addAll(availDinnerManagers);
    availableDinnerStaff.sort(Comparator.comparing(AppUser::getFullName));

    // Saved selections (username values)
    Map<String,String> saved = new HashMap<>();
    assignmentRepo.findByShift_Date(target).forEach(a -> {
      Position pos = a.getShift().getPosition();
      ShiftPeriod per = a.getShift().getPeriod();
      String key = switch (per) {
        case LUNCH -> switch (pos) {
          case LUNCH_SERVER -> "role_LUNCH_SERVER";
          case LUNCH_ASSISTANT -> "role_LUNCH_ASSISTANT";
          case LUNCH_MANAGER -> "role_LUNCH_MANAGER";
          default -> null;
        };
        case DINNER -> switch (pos) {
          case SERVER_1 -> "role_DINNER_SERVER_1";
          case SERVER_2 -> "role_DINNER_SERVER_2";
          case SERVER_3 -> "role_DINNER_SERVER_3";
          case SUSHI    -> "role_DINNER_SUSHI";
          case EXPO     -> "role_DINNER_EXPO";
          case BUSSER_1 -> "role_DINNER_BUSSER_1";
          case BUSSER_2 -> "role_DINNER_BUSSER_2";
          case HOST_1   -> "role_DINNER_HOST_1";
          case HOST_2   -> "role_DINNER_HOST_2";
          case FLOAT    -> "role_DINNER_FLOAT";
          default -> null;
        };
      };
      if (key != null && a.getEmployee() != null) {
        saved.put(key, a.getEmployee().getUsername());
      }
    });

    model.addAttribute("date", target);
    model.addAttribute("prevDate", target.minusDays(1));
    model.addAttribute("nextDate", target.plusDays(1));

    model.addAttribute("lunchRoles", lunchRoles);
    model.addAttribute("dinnerRoles", dinnerRoles);

    model.addAttribute("availableLunchStaff", availableLunchStaff); // employees + managers (available)
    model.addAttribute("availableDinnerStaff", availableDinnerStaff); // employees + managers (available)
    model.addAttribute("availableLunchManagers", availLunchManagers); // managers only (available)
    model.addAttribute("allStaff", allStaff);                         // everyone (override)
    model.addAttribute("allManagers", managers);                      // managers only (override)

    model.addAttribute("saved", saved);
    model.addAttribute("active", "manager-schedule");

    model.addAttribute("locked", isLocked(target));

    return "manager/day";
  }

  private List<AppUser> filterByAvailability(List<AppUser> users, DayOfWeek day, ShiftPeriod period) {
    List<AppUser> out = new ArrayList<>();
    for (AppUser u : users) {
      var opt = availabilityRepo.findByUserAndDayOfWeek(u, day);
      if (opt.isPresent()) {
        var a = opt.get();
        boolean ok = (period == ShiftPeriod.LUNCH) ? a.isLunchAvailable() : a.isDinnerAvailable();
        if (ok) out.add(u);
      }
    }
    out.sort(Comparator.comparing(AppUser::getFullName));
    return out;
  }

  @PostMapping("/schedule/{date}")
  public String saveDay(@PathVariable String date,
                        @RequestParam Map<String,String> params,
                        @RequestParam(name="action", required=false) String action,
                        RedirectAttributes redirectAttributes) {
    LocalDate target = LocalDate.parse(date, DateTimeFormatter.ISO_DATE);

    if (isLocked(target)) {
      redirectAttributes.addFlashAttribute("error", "This period is POSTED. Editing is locked.");
      return "redirect:/manager/schedule/" + date;
    }

    if ("clear".equalsIgnoreCase(action)) {
      assignmentRepo.deleteByShift_Date(target);
      return "redirect:/manager/schedule/{date}?cleared";
    }

    Map<String, RoleOption> roleMap = new HashMap<>();
    for (var ro : List.of(
            new RoleOption("role_LUNCH_SERVER","Server", ShiftPeriod.LUNCH, Position.LUNCH_SERVER),
            new RoleOption("role_LUNCH_ASSISTANT","Assistant", ShiftPeriod.LUNCH, Position.LUNCH_ASSISTANT),
            new RoleOption("role_LUNCH_MANAGER","Manager", ShiftPeriod.LUNCH, Position.LUNCH_MANAGER),
            new RoleOption("role_DINNER_SERVER_1","Server 1", ShiftPeriod.DINNER, Position.SERVER_1),
            new RoleOption("role_DINNER_SERVER_2","Server 2", ShiftPeriod.DINNER, Position.SERVER_2),
            new RoleOption("role_DINNER_SERVER_3","Server 3", ShiftPeriod.DINNER, Position.SERVER_3),
            new RoleOption("role_DINNER_SUSHI","Sushi", ShiftPeriod.DINNER, Position.SUSHI),
            new RoleOption("role_DINNER_EXPO","Expo", ShiftPeriod.DINNER, Position.EXPO),
            new RoleOption("role_DINNER_BUSSER_1","Busser 1", ShiftPeriod.DINNER, Position.BUSSER_1),
            new RoleOption("role_DINNER_BUSSER_2","Busser 2", ShiftPeriod.DINNER, Position.BUSSER_2),
            new RoleOption("role_DINNER_HOST_1","Host 1", ShiftPeriod.DINNER, Position.HOST_1),
            new RoleOption("role_DINNER_HOST_2","Host 2", ShiftPeriod.DINNER, Position.HOST_2),
            new RoleOption("role_DINNER_FLOAT","FLOAT", ShiftPeriod.DINNER, Position.FLOAT)
    )) roleMap.put(ro.key(), ro);

    for (Map.Entry<String,String> e : params.entrySet()) {
      String key = e.getKey();
      if (!key.startsWith("role_")) continue;
      String username = e.getValue();
      RoleOption ro = roleMap.get(key);
      if (ro == null) continue;

      // Enforce: only MANAGERs can be assigned to Lunch Manager role
      if ("role_LUNCH_MANAGER".equals(key)) {
        var userOpt = userRepo.findByUsername(username);
        if (userOpt.isEmpty() || userOpt.get().getRoles().stream().noneMatch(r -> "MANAGER".equals(r.getName()))) {
          continue; // ignore invalid assignment attempt
        }
      }

      Shift shift = shiftRepo.findByDateAndPeriodAndPosition(target, ro.period(), ro.position())
              .orElseGet(() -> {
                Shift s = new Shift();
                s.setDate(target);
                s.setPeriod(ro.period());
                s.setPosition(ro.position());
                return shiftRepo.save(s);
              });

      if (username == null || username.isBlank()) {
        assignmentRepo.findByShift(shift).ifPresent(assignmentRepo::delete);
        continue;
      }

      AppUser user = userRepo.findByUsername(username).orElse(null);
      if (user == null) continue;

      Assignment a = assignmentRepo.findByShift(shift).orElseGet(Assignment::new);
      a.setShift(shift);
      a.setEmployee(user);
      assignmentRepo.save(a);
    }

    return "redirect:/manager/schedule/{date}?saved";
  }
}
