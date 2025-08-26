package com.resto.scheduler.repository;

import com.resto.scheduler.model.AppUser;
import com.resto.scheduler.model.Availability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
  Optional<Availability> findByUserAndDayOfWeek(AppUser user, DayOfWeek day);
  List<Availability> findByUser(AppUser user);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Transactional
  void deleteByUser(AppUser user);
}
