package com.resto.scheduler.service;

import com.resto.scheduler.model.Assignment;
import com.resto.scheduler.model.SchedulePeriod;
import com.resto.scheduler.repository.AssignmentRepository;
import com.resto.scheduler.repository.SchedulePeriodRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class ScheduleViewService {

    private final AssignmentRepository assignments;
    private final SchedulePeriodRepository schedulePeriods;

    public ScheduleViewService(AssignmentRepository assignments,
                               SchedulePeriodRepository schedulePeriods) {
        this.assignments = assignments;
        this.schedulePeriods = schedulePeriods;
    }

    /** Return the Monday for the given date. */
    public LocalDate mondayOf(LocalDate any) {
        return any.with(java.time.DayOfWeek.MONDAY);
    }

    /** Latest POSTED period start date, if any. */
    public Optional<LocalDate> latestPostedStart() {
        return schedulePeriods.findTopByStatusOrderByStartDateDesc("POSTED")
                .map(SchedulePeriod::getStartDate);
    }

    /** Convenience: 7 days starting at weekStart (Mon..Sun). */
    public List<LocalDate> weekDays(LocalDate weekStart) {
        List<LocalDate> days = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) days.add(weekStart.plusDays(i));
        return days;
    }

    /**
     * Build the nested grid your fragment expects:
     *   dateKey (yyyy-MM-dd) -> roleKey (PERIOD_POSITION) -> Assignment
     */
    public Map<String, Map<String, Assignment>> buildAssignmentsGrid(LocalDate startInclusive, LocalDate endInclusive) {
        // Use your existing repo method that you already had in EmployeeController:
        List<Assignment> rows = assignments.findByShift_DateBetween(startInclusive, endInclusive);

        Map<String, Map<String, Assignment>> grid = new HashMap<>();
        for (Assignment a : rows) {
            var s = a.getShift();
            if (s == null) continue;

            String dateKey = s.getDate().toString(); // yyyy-MM-dd
            String roleKey = s.getPeriod().name() + "_" + s.getPosition().name(); // e.g. DINNER_SERVER_1

            grid.computeIfAbsent(dateKey, k -> new HashMap<>()).put(roleKey, a);
        }
        return grid;
    }

    /** Holder for a two-week view (optional helper if you need it later). */
    public record TwoWeekView(LocalDate startDate,
                              List<LocalDate> week1Days,
                              List<LocalDate> week2Days,
                              Map<String, Map<String, Assignment>> assignmentsGrid) {}

    /** Build a two-week view (Mon..Sun x 2) with one combined assignments grid. */
    public TwoWeekView twoWeek(LocalDate periodStart) {
        List<LocalDate> week1 = weekDays(periodStart);
        List<LocalDate> week2 = weekDays(periodStart.plusWeeks(1));
        Map<String, Map<String, Assignment>> grid = buildAssignmentsGrid(periodStart, periodStart.plusDays(13));
        return new TwoWeekView(periodStart, week1, week2, grid);
    }
}
