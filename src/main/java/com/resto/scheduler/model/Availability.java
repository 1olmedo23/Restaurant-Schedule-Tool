package com.resto.scheduler.model;

import jakarta.persistence.*;
import java.time.DayOfWeek;

@Entity
@Table(name = "availability",
        uniqueConstraints = @UniqueConstraint(name="uk_avail_user_day", columnNames={"user_id","day_of_week"}))
public class Availability {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name="user_id")
  private AppUser user;

  @Enumerated(EnumType.STRING)
  @Column(name="day_of_week", nullable=false)
  private DayOfWeek dayOfWeek;

  @Column(name="lunch_available")
  private boolean lunchAvailable;

  @Column(name="dinner_available")
  private boolean dinnerAvailable;

  public Long getId(){return id;}
  public void setId(Long id){this.id=id;}
  public AppUser getUser(){return user;}
  public void setUser(AppUser user){this.user=user;}
  public DayOfWeek getDayOfWeek(){return dayOfWeek;}
  public void setDayOfWeek(DayOfWeek dayOfWeek){this.dayOfWeek=dayOfWeek;}
  public boolean isLunchAvailable(){return lunchAvailable;}
  public void setLunchAvailable(boolean lunchAvailable){this.lunchAvailable=lunchAvailable;}
  public boolean isDinnerAvailable(){return dinnerAvailable;}
  public void setDinnerAvailable(boolean dinnerAvailable){this.dinnerAvailable=dinnerAvailable;}
}