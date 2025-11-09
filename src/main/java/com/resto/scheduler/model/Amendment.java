package com.resto.scheduler.model;

import com.resto.scheduler.model.enums.Position;
import com.resto.scheduler.model.enums.ShiftPeriod;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "amendment",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_amendment",
                columnNames = {"schedule_period_id", "date", "period", "position"}
        )
)
public class Amendment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "schedule_period_id")
    private SchedulePeriod schedulePeriod;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ShiftPeriod period;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Position position;

    @ManyToOne
    @JoinColumn(name = "original_employee_id")
    private AppUser originalEmployee;

    @ManyToOne
    @JoinColumn(name = "new_employee_id")
    private AppUser newEmployee;

    @ManyToOne
    @JoinColumn(name = "changed_by")
    private AppUser changedBy;

    @Column(nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    // Getters & setters
    public Long getId() { return id; }
    public SchedulePeriod getSchedulePeriod() { return schedulePeriod; }
    public void setSchedulePeriod(SchedulePeriod schedulePeriod) { this.schedulePeriod = schedulePeriod; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public ShiftPeriod getPeriod() { return period; }
    public void setPeriod(ShiftPeriod period) { this.period = period; }
    public Position getPosition() { return position; }
    public void setPosition(Position position) { this.position = position; }
    public AppUser getOriginalEmployee() { return originalEmployee; }
    public void setOriginalEmployee(AppUser originalEmployee) { this.originalEmployee = originalEmployee; }
    public AppUser getNewEmployee() { return newEmployee; }
    public void setNewEmployee(AppUser newEmployee) { this.newEmployee = newEmployee; }
    public AppUser getChangedBy() { return changedBy; }
    public void setChangedBy(AppUser changedBy) { this.changedBy = changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
