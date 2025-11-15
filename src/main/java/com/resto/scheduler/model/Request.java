package com.resto.scheduler.model;

import com.resto.scheduler.model.enums.RequestStatus;
import com.resto.scheduler.model.enums.RequestType;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "request")
public class Request {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RequestType type; // TIME_OFF or TRADE

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RequestStatus status = RequestStatus.PENDING;

    @ManyToOne(optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private AppUser requester;

    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @ManyToOne
    @JoinColumn(name = "decided_by")
    private AppUser decidedBy; // manager who approved/denied

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "note", length = 1000)
    private String note; // optional manager note

    // ---- Trade-only fields ----
    @ManyToOne
    @JoinColumn(name = "offer_assignment_id")
    private Assignment offerAssignment; // requester’s assignment (must exist for TRADE)

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private AppUser receiver; // target employee to receive the trade

    @Column(name = "receiver_confirmed")
    private boolean receiverConfirmed = false;

    @Column(name = "receiver_confirmed_at")
    private Instant receiverConfirmedAt;

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RequestType getType() { return type; }
    public void setType(RequestType type) { this.type = type; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public AppUser getRequester() { return requester; }
    public void setRequester(AppUser requester) { this.requester = requester; }

    public LocalDate getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDate requestDate) { this.requestDate = requestDate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public AppUser getDecidedBy() { return decidedBy; }
    public void setDecidedBy(AppUser decidedBy) { this.decidedBy = decidedBy; }

    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Assignment getOfferAssignment() { return offerAssignment; }
    public void setOfferAssignment(Assignment offerAssignment) { this.offerAssignment = offerAssignment; }

    public AppUser getReceiver() { return receiver; }
    public void setReceiver(AppUser receiver) { this.receiver = receiver; }

    public boolean isReceiverConfirmed() { return receiverConfirmed; }
    public void setReceiverConfirmed(boolean receiverConfirmed) { this.receiverConfirmed = receiverConfirmed; }

    public Instant getReceiverConfirmedAt() { return receiverConfirmedAt; }
    public void setReceiverConfirmedAt(Instant receiverConfirmedAt) { this.receiverConfirmedAt = receiverConfirmedAt; }
}
