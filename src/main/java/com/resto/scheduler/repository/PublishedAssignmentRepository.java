package com.resto.scheduler.repository;

import com.resto.scheduler.model.PublishedAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PublishedAssignmentRepository extends JpaRepository<PublishedAssignment, Long> {

    List<PublishedAssignment> findBySchedulePeriod_Id(Long schedulePeriodId);

    List<PublishedAssignment> findByDateBetween(LocalDate start, LocalDate end);

    List<PublishedAssignment> findBySchedulePeriod_IdAndDateBetween(Long schedulePeriodId,
                                                                    LocalDate start, LocalDate end);
    long countBySchedulePeriod_Id(Long schedulePeriodId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PublishedAssignment p where p.schedulePeriod.id = :spid")
    void deleteAllBySchedulePeriodId(@Param("spid") Long schedulePeriodId);
}

