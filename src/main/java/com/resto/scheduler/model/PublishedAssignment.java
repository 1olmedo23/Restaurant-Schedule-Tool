package com.resto.scheduler.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import com.resto.scheduler.model.enums.ShiftPeriod;
import com.resto.scheduler.model.enums.Position;

@Entity
@Table(
        name = "published_assignment",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_published",
                columnNames = {"schedule_period_id","date","period","position"}
        )
)
public class PublishedAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "schedule_period_id", nullable = false)
    private SchedulePeriod schedulePeriod;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false, length = 16)
    private ShiftPeriod period;

    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false, length = 32)
    private Position position;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user; // can be null

    // getters/setters
    public Long getId() { return id; }

    public SchedulePeriod getSchedulePeriod() { return schedulePeriod; }
    public void setSchedulePeriod(SchedulePeriod schedulePeriod) { this.schedulePeriod = schedulePeriod; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public ShiftPeriod getPeriod() { return period; }
    public void setPeriod(ShiftPeriod period) { this.period = period; }

    public Position getPosition() { return position; }
    public void setPosition(Position position) { this.position = position; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
}

