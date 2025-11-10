package com.resto.scheduler.repository;
import com.resto.scheduler.model.Shift;
import com.resto.scheduler.model.enums.Position;
import com.resto.scheduler.model.enums.ShiftPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;
public interface ShiftRepository extends JpaRepository<Shift, Long> {
    Optional<Shift> findByDateAndPeriodAndPosition(LocalDate date, ShiftPeriod period, Position position);
    List<Shift> findByDate(LocalDate date);

    List<Shift> findByDateBetween(LocalDate start, LocalDate end);
}