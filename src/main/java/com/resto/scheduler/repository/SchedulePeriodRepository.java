package com.resto.scheduler.repository;

import com.resto.scheduler.model.SchedulePeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SchedulePeriodRepository extends JpaRepository<SchedulePeriod, Long> {

    Optional<SchedulePeriod> findByStartDate(LocalDate startDate);

    // All POSTED periods, newest first
    List<SchedulePeriod> findAllByStatusOrderByStartDateDesc(String status);

    // Latest POSTED
    Optional<SchedulePeriod> findTopByStatusOrderByStartDateDesc(String status);

    // POSTED that contains a given date (handy utility)
    Optional<SchedulePeriod> findTopByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            String status, LocalDate onOrBefore, LocalDate onOrAfter);

    // Prev/Next POSTED relative to a start date
    Optional<SchedulePeriod> findFirstByStatusAndStartDateLessThanOrderByStartDateDesc(
            String status, LocalDate startExclusive);

    Optional<SchedulePeriod> findFirstByStatusAndStartDateGreaterThanOrderByStartDateAsc(
            String status, LocalDate startExclusive);

    // Find POSTED periods that overlap a date range (for coloring the builder)
    @Query("""
  select p from SchedulePeriod p
  where lower(p.status) = 'posted'
    and p.startDate <= :end
    and p.endDate   >= :start
  order by p.startDate asc
""")
    java.util.List<com.resto.scheduler.model.SchedulePeriod> findPostedOverlapping(
            @Param("start") java.time.LocalDate start,
            @Param("end")   java.time.LocalDate end);
}
