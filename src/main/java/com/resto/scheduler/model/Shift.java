package com.resto.scheduler.model;

import com.resto.scheduler.model.enums.Position;
import com.resto.scheduler.model.enums.ShiftPeriod;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="shift",
        uniqueConstraints = @UniqueConstraint(name="uk_shift_date_period_position",
                columnNames={"date","period","position"}))
public class Shift {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private LocalDate date;

  @Enumerated(EnumType.STRING)
  private ShiftPeriod period;

  @Enumerated(EnumType.STRING)
  private Position position;

  public Long getId(){return id;} public void setId(Long id){this.id=id;}
  public LocalDate getDate(){return date;} public void setDate(LocalDate date){this.date=date;}
  public ShiftPeriod getPeriod(){return period;} public void setPeriod(ShiftPeriod period){this.period=period;}
  public Position getPosition(){return position;} public void setPosition(Position position){this.position=position;}
}
