package com.resto.scheduler.model;

import com.resto.scheduler.model.enums.RequestStatus;
import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

@Entity
@Table(name = "availability",
        uniqueConstraints = @UniqueConstraint(name = "uk_avail_user_day", columnNames = {"user_id", "day_of_week"}))
public class Availability {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id")
  private AppUser user;

  @Enumerated(EnumType.STRING)
  @Column(name = "day_of_week", nullable = false)
  private DayOfWeek dayOfWeek;

  // === Canonical (APPROVED) values used by schedule builder ===
  @Column(name = "lunch_available")
  private boolean lunchAvailable;

  @Column(name = "dinner_available")
  private boolean dinnerAvailable;

  // === Requested values (what the user last submitted) ===
  @Column(name = "requested_lunch_available")
  private Boolean requestedLunchAvailable;

  @Column(name = "requested_dinner_available")
  private Boolean requestedDinnerAvailable;

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  private RequestStatus status = RequestStatus.PENDING;

  @Column(name = "submitted_at")
  private LocalDateTime submittedAt;

  @Column(name = "decided_at")
  private LocalDateTime decidedAt;

  // === Getters / setters ===
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public AppUser getUser() {
    return user;
  }

  public void setUser(AppUser user) {
    this.user = user;
  }

  public DayOfWeek getDayOfWeek() {
    return dayOfWeek;
  }

  public void setDayOfWeek(DayOfWeek dayOfWeek) {
    this.dayOfWeek = dayOfWeek;
  }

  // Approved values (used by schedule builder)
  public boolean isLunchAvailable() {
    return lunchAvailable;
  }

  public void setLunchAvailable(boolean lunchAvailable) {
    this.lunchAvailable = lunchAvailable;
  }

  public boolean isDinnerAvailable() {
    return dinnerAvailable;
  }

  public void setDinnerAvailable(boolean dinnerAvailable) {
    this.dinnerAvailable = dinnerAvailable;
  }

  // Requested values (latest employee request)
  public Boolean getRequestedLunchAvailable() {
    return requestedLunchAvailable;
  }

  public void setRequestedLunchAvailable(Boolean requestedLunchAvailable) {
    this.requestedLunchAvailable = requestedLunchAvailable;
  }

  public Boolean getRequestedDinnerAvailable() {
    return requestedDinnerAvailable;
  }

  public void setRequestedDinnerAvailable(Boolean requestedDinnerAvailable) {
    this.requestedDinnerAvailable = requestedDinnerAvailable;
  }

  public RequestStatus getStatus() {
    return status;
  }

  public void setStatus(RequestStatus status) {
    this.status = status;
  }

  public LocalDateTime getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(LocalDateTime submittedAt) {
    this.submittedAt = submittedAt;
  }

  public LocalDateTime getDecidedAt() {
    return decidedAt;
  }

  public void setDecidedAt(LocalDateTime decidedAt) {
    this.decidedAt = decidedAt;
  }
}
