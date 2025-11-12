package com.resto.scheduler.repository;

import com.resto.scheduler.model.Amendment;
import com.resto.scheduler.model.enums.Position;
import com.resto.scheduler.model.enums.ShiftPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AmendmentRepository extends JpaRepository<Amendment, Long> {

    // For coloring/tags in the 14-day builder
    List<Amendment> findByDateBetween(LocalDate start, LocalDate end);

    // For upserting a single role cell’s amendment
    Optional<Amendment> findBySchedulePeriod_IdAndDateAndPeriodAndPosition(
            Long schedulePeriodId, LocalDate date, ShiftPeriod period, Position position
    );

    List<Amendment> findByDate(LocalDate date);

    // Returns true if at least one amendment exists for the given period
    boolean existsBySchedulePeriod_Id(Long schedulePeriodId);
}
