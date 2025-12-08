package com.resto.scheduler.service;

import com.resto.scheduler.model.*;
import com.resto.scheduler.model.enums.NotificationType;
import com.resto.scheduler.model.enums.RequestStatus;
import com.resto.scheduler.model.enums.RequestType;
import com.resto.scheduler.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import com.resto.scheduler.model.enums.ShiftPeriod;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RequestService {

    private final RequestRepository requestRepo;
    private final AssignmentRepository assignmentRepo;
    private final NotificationRepository notificationRepo;
    private final SchedulePeriodRepository schedulePeriodRepo;
    private final AmendmentRepository amendmentRepo;
    private final AppUserRepository userRepo;

    // for “open” checks if you want later
    private static final List<RequestStatus> OPEN_STATUSES =
            List.of(RequestStatus.PENDING);

    private static final DateTimeFormatter REQ_DATE_FMT =
            DateTimeFormatter.ofPattern("MM-dd-yy");

    public RequestService(RequestRepository requestRepo,
                          AssignmentRepository assignmentRepo,
                          NotificationRepository notificationRepo,
                          SchedulePeriodRepository schedulePeriodRepo,
                          AmendmentRepository amendmentRepo,
                          AppUserRepository userRepo) {
        this.requestRepo = requestRepo;
        this.assignmentRepo = assignmentRepo;
        this.notificationRepo = notificationRepo;
        this.schedulePeriodRepo = schedulePeriodRepo;
        this.amendmentRepo = amendmentRepo;
        this.userRepo = userRepo;
    }

    /**
     * Employee/Manager creates a request by date.
     * If the requester has an assignment that day, we FORCE TRADE (requires receiver).
     * Otherwise we create TIME_OFF.
     */
    @Transactional
    public Request createTimeOffOrTradeAuto(AppUser requester, LocalDate date, String receiverUsername) {
        // Can have 0, 1, or 2+ assignments for that date
        List<Assignment> assignments = assignmentRepo.findByEmployeeAndShift_Date(requester, date);

        if (assignments.isEmpty()) {
            // No assignment that day → pure time-off request
            Request r = new Request();
            r.setType(RequestType.TIME_OFF);
            r.setStatus(RequestStatus.PENDING);
            r.setRequester(requester);
            r.setRequestDate(date);
            r.setCreatedAt(Instant.now());
            return requestRepo.save(r);
        }

        // Already scheduled that day → this must be a trade
        if (receiverUsername == null || receiverUsername.isBlank()) {
            // Controller catches this and shows the "please request a trade" banner
            throw new IllegalArgumentException("You have been assigned for this date, submit a trade request.");
        }

        AppUser receiver = userRepo.findByUsername(receiverUsername)
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found."));

        // For simplicity, if they somehow have multiple shifts that day, we trade the first one
        Assignment offer = assignments.get(0);

        Request r = new Request();
        r.setType(RequestType.TRADE);
        r.setStatus(RequestStatus.PENDING);
        r.setRequester(requester);
        r.setReceiver(receiver);
        r.setRequestDate(date);
        r.setCreatedAt(Instant.now());
        r.setOfferAssignment(offer);
        r.setReceiverConfirmed(false);
        // r.setNote(null); // optional if you have note, otherwise omit
        return requestRepo.save(r);
    }

    /**
     * Explicit trade request (the "Request Trade" button).
     */
    @Transactional
    public Request createTrade(AppUser requester,
                               LocalDate date,
                               ShiftPeriod period,
                               String receiverUsername) {
        if (receiverUsername == null || receiverUsername.isBlank()) {
            throw new IllegalArgumentException("Receiver is required for trade request.");
        }
        if (period == null) {
            throw new IllegalArgumentException("Shift period (LUNCH or DINNER) is required for trade request.");
        }

        AppUser receiver = userRepo.findByUsername(receiverUsername)
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found."));

        // All assignments for that day
        List<Assignment> assignments = assignmentRepo.findByEmployeeAndShift_Date(requester, date);

        if (assignments.isEmpty()) {
            throw new IllegalArgumentException("You are not scheduled on this day.");
        }

        // Find the assignment that matches the selected period (LUNCH or DINNER)
        Assignment offer = assignments.stream()
                .filter(a -> a.getShift() != null && a.getShift().getPeriod() == period)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "You are not scheduled for the selected shift (" + period + ") on this day."
                ));

        Request r = new Request();
        r.setType(RequestType.TRADE);
        r.setStatus(RequestStatus.PENDING);
        r.setRequester(requester);
        r.setReceiver(receiver);
        r.setRequestDate(date);
        r.setCreatedAt(Instant.now());
        r.setOfferAssignment(offer);
        r.setReceiverConfirmed(false);
        // r.setNote(null); // optional

        return requestRepo.save(r);
    }

    /**
     * Legacy helper if you still call this anywhere else.
     * Disallow TIME_OFF if they actually have an assignment that day.
     */
    @Transactional
    public Request createTimeOff(AppUser requester, LocalDate date) {
        // now returns List<Assignment>, so check empty()
        if (!assignmentRepo.findByEmployeeAndShift_Date(requester, date).isEmpty()) {
            throw new IllegalArgumentException("You are assigned that day; request a trade instead.");
        }
        return createTimeOffInternal(requester, date);
    }

    private Request createTradeInternal(AppUser requester, LocalDate date, Assignment offerAssignment, AppUser receiver) {
        Request r = new Request();
        r.setType(RequestType.TRADE);
        r.setStatus(RequestStatus.PENDING);
        r.setRequester(requester);
        r.setRequestDate(date);
        r.setOfferAssignment(offerAssignment);
        r.setReceiver(receiver);
        r.setCreatedAt(Instant.now());
        r = requestRepo.save(r);

        String dateStr = REQ_DATE_FMT.format(date);

        notify(receiver, NotificationType.TRADE_INVITE,
                "Trade request from " + requester.getFullName() + " for " + dateStr + " (request #" + r.getId() + ")");
        notify(requester, NotificationType.REQ_CREATED,
                "Trade request submitted for " + dateStr + " (request #" + r.getId() + ")");
        return r;
    }

    private Request createTimeOffInternal(AppUser requester, LocalDate date) {
        Request r = new Request();
        r.setType(RequestType.TIME_OFF);
        r.setStatus(RequestStatus.PENDING);
        r.setRequester(requester);
        r.setRequestDate(date);
        r.setCreatedAt(Instant.now());
        r = requestRepo.save(r);

        String dateStr = REQ_DATE_FMT.format(date);

        notify(requester, NotificationType.REQ_CREATED,
                "Time off request submitted for " + dateStr + " (request #" + r.getId() + ")");
        return r;
    }

    @Transactional
    public Request receiverConfirm(Request request, AppUser receiver) {
        if (request.getType() != RequestType.TRADE) {
            throw new IllegalStateException("Only trade requests support receiver confirmation.");
        }
        if (!Objects.equals(request.getReceiver().getId(), receiver.getId())) {
            throw new SecurityException("Only the designated receiver can confirm this trade.");
        }
        request.setReceiverConfirmed(true);
        request.setReceiverConfirmedAt(Instant.now());
        Request saved = requestRepo.save(request);

        String dateStr = REQ_DATE_FMT.format(request.getRequestDate());

        notify(request.getRequester(), NotificationType.REQ_UPDATED,
                "Your trade request for " + dateStr + " was confirmed by " + receiver.getFullName());
        return saved;
    }

    @Transactional
    public Request approve(Request request, AppUser manager, String note) {
        request.setStatus(RequestStatus.APPROVED);
        request.setDecidedBy(manager);
        request.setDecidedAt(Instant.now());
        request.setNote(note);
        Request saved = requestRepo.save(request);

        // Apply effects
        if (request.getType() == RequestType.TRADE) {
            applyTradeEffect(request);
        } else if (request.getType() == RequestType.TIME_OFF) {
            // No schedule mutation; badge will display on day page and builder.
        }

        // Notifications
        notify(request.getRequester(), NotificationType.TRADE_DECISION,
                label(request) + " approved" + (note != null && !note.isBlank() ? (": " + note) : ""));
        if (request.getReceiver() != null) {
            notify(request.getReceiver(), NotificationType.TRADE_DECISION,
                    label(request) + " approved" + (note != null && !note.isBlank() ? (": " + note) : ""));
        }
        return saved;
    }

    @Transactional
    public Request deny(Request request, AppUser manager, String note) {
        request.setStatus(RequestStatus.DENIED);
        request.setDecidedBy(manager);
        request.setDecidedAt(Instant.now());
        request.setNote(note);
        Request saved = requestRepo.save(request);

        notify(request.getRequester(), NotificationType.TRADE_DECISION,
                label(request) + " denied" + (note != null && !note.isBlank() ? (": " + note) : ""));
        if (request.getReceiver() != null) {
            notify(request.getReceiver(), NotificationType.TRADE_DECISION,
                    label(request) + " denied" + (note != null && !note.isBlank() ? (": " + note) : ""));
        }
        return saved;
    }

    private String label(Request r) {
        String dateStr = REQ_DATE_FMT.format(r.getRequestDate());
        return (r.getType() == RequestType.TRADE ? "Trade request" : "Time off request")
                + " for " + dateStr;
    }

    /**
     * Change the assignment’s employee to receiver.
     * If the date belongs to a POSTED period, record an Amendment so UI shows "Edited".
     */
    /**
     * Change the assignment’s employee to receiver.
     * If the date belongs to a POSTED period, record an Amendment so UI shows "Edited".
     */
    private void applyTradeEffect(Request r) {
        Assignment offer = r.getOfferAssignment();
        if (offer == null || r.getReceiver() == null) {
            return;
        }

        Shift shift = offer.getShift();
        LocalDate date = shift.getDate();

        AppUser original = offer.getEmployee();

        // 1) Reassign to receiver
        offer.setEmployee(r.getReceiver());
        assignmentRepo.save(offer);

        // 2) If the date is in a POSTED period, record an Amendment and attach that period
        schedulePeriodRepo.findPostedContaining(date).ifPresent(period -> {
            Amendment a = new Amendment();
            a.setSchedulePeriod(period);          // IMPORTANT: link to SchedulePeriod (NOT NULL in DB)
            a.setDate(date);
            a.setPeriod(shift.getPeriod());
            a.setPosition(shift.getPosition());
            a.setOriginalEmployee(original);
            a.setNewEmployee(r.getReceiver());
            // changedAt / changedBy are handled by your entity annotations / defaults, so we don't touch them here
            amendmentRepo.save(a);
        });
    }

    public List<Request> listForUser(AppUser u) {
        return requestRepo.findByRequesterOrderByCreatedAtDesc(u);
    }

    public List<Request> listPending(LocalDate start, LocalDate end) {
        // PENDING requests in the date range
        List<Request> pending = requestRepo.findByStatusAndRequestDateBetween(
                RequestStatus.PENDING, start, end
        );

        // Oldest first, so manager can see who requested earlier
        pending.sort(Comparator.comparing(Request::getCreatedAt));

        return pending;
    }

    /**
     * All decided requests (APPROVED + DENIED) in the date range,
     * newest decisions first (for manager history view).
     */
    public List<Request> listDecided(LocalDate start, LocalDate end) {
        List<Request> approved = requestRepo.findByStatusAndRequestDateBetween(
                RequestStatus.APPROVED, start, end
        );
        List<Request> denied = requestRepo.findByStatusAndRequestDateBetween(
                RequestStatus.DENIED, start, end
        );

        List<Request> all = new ArrayList<>();
        all.addAll(approved);
        all.addAll(denied);

        // Sort by decidedAt, newest first. Nulls (shouldn't happen) go last.
        all.sort(
                Comparator.comparing(
                        Request::getDecidedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed()
        );

        return all;
    }

    /**
     * Helper for badges on the schedule-builder grid/day page.
     * Map<date, List<employee full name>>
     */
    public Map<LocalDate, List<String>> getApprovedTimeOffByDate(LocalDate start, LocalDate end) {
        List<Request> approved = requestRepo.findByTypeAndStatusAndRequestDateBetween(
                RequestType.TIME_OFF, RequestStatus.APPROVED, start, end);
        return approved.stream().collect(Collectors.groupingBy(
                Request::getRequestDate,
                Collectors.mapping(r -> r.getRequester().getFullName(), Collectors.toList())
        ));
    }

    /**
     * Used by day page to hide people who have approved time off that day.
     */
    public boolean hasApprovedTimeOff(AppUser user, LocalDate date) {
        Map<LocalDate, List<String>> map = getApprovedTimeOffByDate(date, date);
        List<String> names = map.get(date);
        if (names == null) return false;
        String full = user.getFullName();
        return names.stream().anyMatch(n -> n.equals(full));
    }

    private void notify(AppUser recipient, NotificationType type, String payload) {
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setType(type);
        n.setPayload(payload);
        notificationRepo.save(n);
    }
}
