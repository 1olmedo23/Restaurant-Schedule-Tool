package com.resto.scheduler.service;

import com.resto.scheduler.model.SchedulePeriod;
import com.resto.scheduler.repository.SchedulePeriodRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class ScheduleViewService {

    private final SchedulePeriodRepository periodRepo;
    private static final String POSTED = "POSTED";

    public ScheduleViewService(SchedulePeriodRepository periodRepo) {
        this.periodRepo = periodRepo;
    }

    /** Align any date to Monday (start of week). */
    public LocalDate mondayOf(LocalDate date) {
        LocalDate d = date;
        while (d.getDayOfWeek() != DayOfWeek.MONDAY) d = d.minusDays(1);
        return d;
    }

    /** Latest posted period start date (if any). */
    public Optional<LocalDate> latestPostedStart() {
        return periodRepo.findTopByStatusOrderByStartDateDesc(POSTED)
                .map(SchedulePeriod::getStartDate);
    }

    /** Given a posted start date, find the previous posted period's start. */
    public Optional<LocalDate> previousPostedStart(LocalDate start) {
        return periodRepo.findFirstByStatusAndStartDateLessThanOrderByStartDateDesc(POSTED, start)
                .map(SchedulePeriod::getStartDate);
    }

    /** Given a posted start date, find the next posted period's start. */
    public Optional<LocalDate> nextPostedStart(LocalDate start) {
        return periodRepo.findFirstByStatusAndStartDateGreaterThanOrderByStartDateAsc(POSTED, start)
                .map(SchedulePeriod::getStartDate);
    }

    /** True if the given start belongs to a posted period. */
    public boolean isPostedStart(LocalDate start) {
        return periodRepo.findByStartDate(start)
                .map(SchedulePeriod::getStatus)
                .map(s -> POSTED.equalsIgnoreCase(s))
                .orElse(false);
    }
}
