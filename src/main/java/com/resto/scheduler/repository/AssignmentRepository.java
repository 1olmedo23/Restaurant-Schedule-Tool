package com.resto.scheduler.repository;

import com.resto.scheduler.model.AppUser;
import com.resto.scheduler.model.Assignment;
import com.resto.scheduler.model.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    Optional<Assignment> findByShift(Shift shift);

    List<Assignment> findByShift_Date(LocalDate date);

    void deleteByShift_Date(LocalDate date);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    void deleteByEmployee(AppUser employee);

    List<Assignment> findByEmployeeAndShift_DateBetween(AppUser employee, LocalDate start, LocalDate end);

    // NEW: fetch ALL assignments across a date range (for the fixed grid)
    List<Assignment> findByShift_DateBetween(LocalDate start, LocalDate end);
}
