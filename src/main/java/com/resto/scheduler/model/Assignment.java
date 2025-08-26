package com.resto.scheduler.model;

import jakarta.persistence.*;

@Entity
@Table(name="assignment",
        uniqueConstraints = @UniqueConstraint(name="uk_assignment_shift", columnNames={"shift_id"}))
public class Assignment {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional=false)
  @JoinColumn(name="shift_id")
  private Shift shift;

  @ManyToOne(optional=false)
  @JoinColumn(name="user_id")
  private AppUser employee;

  public Long getId(){return id;} public void setId(Long id){this.id=id;}
  public Shift getShift(){return shift;} public void setShift(Shift shift){this.shift=shift;}
  public AppUser getEmployee(){return employee;} public void setEmployee(AppUser employee){this.employee=employee;}
}
