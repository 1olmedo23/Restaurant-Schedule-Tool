package com.resto.scheduler.model;

import com.resto.scheduler.model.enums.NotificationType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notification", indexes = {
        @Index(name = "idx_notification_recipient_created", columnList = "recipient_id, created_at")
})
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private AppUser recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationType type;

    @Column(name = "payload", length = 2000)
    private String payload; // simple JSON/text blob with details (request id, date, etc.)

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AppUser getRecipient() { return recipient; }
    public void setRecipient(AppUser recipient) { this.recipient = recipient; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
