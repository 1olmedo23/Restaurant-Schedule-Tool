package com.resto.scheduler.repository;

import com.resto.scheduler.model.SchedulePeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SchedulePeriodRepository extends JpaRepository<SchedulePeriod, Long> {
    Optional<SchedulePeriod> findByStartDate(LocalDate startDate);
    Optional<SchedulePeriod> findTopByStatusOrderByStartDateDesc(String status);
}
