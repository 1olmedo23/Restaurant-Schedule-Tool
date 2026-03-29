package com.resto.scheduler.service.impl;

import com.resto.scheduler.model.PublishedAssignment;
import com.resto.scheduler.model.SchedulePeriod;
import com.resto.scheduler.model.Shift;
import com.resto.scheduler.repository.AssignmentRepository;
import com.resto.scheduler.repository.PublishedAssignmentRepository;
import com.resto.scheduler.repository.SchedulePeriodRepository;
import com.resto.scheduler.repository.ShiftRepository;
import com.resto.scheduler.service.PublishService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.resto.scheduler.model.AppUser;
import com.resto.scheduler.model.Assignment;
import com.resto.scheduler.model.enums.NotificationType;
import com.resto.scheduler.repository.AppUserRepository;
import com.resto.scheduler.service.NotificationDeliveryService;
import org.springframework.beans.factory.annotation.Value;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PublishServiceImpl implements PublishService {

    private final SchedulePeriodRepository periodRepo;
    private final ShiftRepository shiftRepo;
    private final AssignmentRepository assignmentRepo;
    private final PublishedAssignmentRepository publishedRepo;

    private final AppUserRepository appUserRepo;
    private final NotificationDeliveryService notificationDeliveryService;

    @Value("${app.public-login-url}")
    private String publicLoginUrl;

    private static final DateTimeFormatter PUBLISH_DATE_FMT =
            DateTimeFormatter.ofPattern("MM-dd-yy");

    public PublishServiceImpl(SchedulePeriodRepository periodRepo,
                              ShiftRepository shiftRepo,
                              AssignmentRepository assignmentRepo,
                              PublishedAssignmentRepository publishedRepo,
                              AppUserRepository appUserRepo,
                              NotificationDeliveryService notificationDeliveryService) {
        this.periodRepo = periodRepo;
        this.shiftRepo = shiftRepo;
        this.assignmentRepo = assignmentRepo;
        this.publishedRepo = publishedRepo;
        this.appUserRepo = appUserRepo;
        this.notificationDeliveryService = notificationDeliveryService;
    }

    @Override
    @Transactional
    public void snapshotPeriod(Long schedulePeriodId) {
        SchedulePeriod sp = periodRepo.findById(schedulePeriodId).orElseThrow();
        if (!"POSTED".equalsIgnoreCase(sp.getStatus())) {
            return;
        }

        // Determine whether this is the first publish or a republish
        List<PublishedAssignment> existingPublishedRows = publishedRepo.findBySchedulePeriod_Id(sp.getId());
        boolean firstPublish = existingPublishedRows.isEmpty();

        // Build old published user map BEFORE deleting old snapshot rows
        record Key(LocalDate d, String per, String pos) {}
        Map<Key, Long> oldPublishedMap = new HashMap<>();
        Set<Long> oldPublishedUserIds = new HashSet<>();

        for (PublishedAssignment pa : existingPublishedRows) {
            Key key = new Key(pa.getDate(), pa.getPeriod().name(), pa.getPosition().name());
            Long userId = pa.getUser() != null ? pa.getUser().getId() : null;
            oldPublishedMap.put(key, userId);
            if (userId != null) {
                oldPublishedUserIds.add(userId);
            }
        }

        // Build current/live map from shifts + assignments
        Map<Key, Long> newPublishedMap = new HashMap<>();
        Set<Long> newPublishedUserIds = new HashSet<>();

        var shifts = shiftRepo.findByDateBetween(sp.getStartDate(), sp.getEndDate());
        for (Shift s : shifts) {
            var aOpt = assignmentRepo.findByShift(s);
            if (aOpt.isEmpty()) {
                continue;
            }

            Assignment a = aOpt.get();
            AppUser employee = a.getEmployee();
            Long userId = employee != null ? employee.getId() : null;

            Key key = new Key(s.getDate(), s.getPeriod().name(), s.getPosition().name());
            newPublishedMap.put(key, userId);

            if (userId != null) {
                newPublishedUserIds.add(userId);
            }
        }

        // Replace snapshot rows
        publishedRepo.deleteAllBySchedulePeriodId(sp.getId());

        for (Shift s : shifts) {
            var aOpt = assignmentRepo.findByShift(s);
            if (aOpt.isEmpty()) {
                continue;
            }

            Assignment a = aOpt.get();

            PublishedAssignment pub = new PublishedAssignment();
            pub.setSchedulePeriod(sp);
            pub.setDate(s.getDate());
            pub.setPeriod(s.getPeriod());
            pub.setPosition(s.getPosition());
            pub.setUser(a.getEmployee());
            publishedRepo.save(pub);
        }

        // Notify users
        if (firstPublish) {
            notifyAllUsersForFirstPublish(sp);
        } else {
            notifyAffectedUsersForRepublish(sp, oldPublishedMap, newPublishedMap, oldPublishedUserIds, newPublishedUserIds);
        }
    }

    @Override
    @Transactional
    public void snapshotLatestPosted() {
        periodRepo.findTopByStatusOrderByStartDateDesc("POSTED")
                .ifPresent(sp -> snapshotPeriod(sp.getId()));
    }

    private void notifyAllUsersForFirstPublish(SchedulePeriod sp) {
        Set<AppUser> recipients = new HashSet<>();
        recipients.addAll(appUserRepo.findByRoles_Name("EMPLOYEE"));
        recipients.addAll(appUserRepo.findByRoles_Name("MANAGER"));

        String range = sp.getStartDate().format(PUBLISH_DATE_FMT) + " to " + sp.getEndDate().format(PUBLISH_DATE_FMT);
        String payload = "New schedule published for " + range;
        String smsMessage = "SKT Scheduler: A new schedule was published for " + range
                + ". Log in to view: " + publicLoginUrl;

        for (AppUser user : recipients) {
            if (user == null || !user.isEnabled()) {
                continue;
            }

            notificationDeliveryService.notifyInAppAndSms(
                    user,
                    NotificationType.SCHEDULE_PUBLISHED,
                    payload,
                    smsMessage
            );
        }
    }

    private void notifyAffectedUsersForRepublish(SchedulePeriod sp,
                                                 Map<?, Long> oldPublishedMap,
                                                 Map<?, Long> newPublishedMap,
                                                 Set<Long> oldPublishedUserIds,
                                                 Set<Long> newPublishedUserIds) {
        Set<Long> affectedUserIds = new HashSet<>();

        Set<Object> allKeys = new HashSet<>();
        allKeys.addAll(oldPublishedMap.keySet());
        allKeys.addAll(newPublishedMap.keySet());

        for (Object key : allKeys) {
            Long oldUserId = oldPublishedMap.get(key);
            Long newUserId = newPublishedMap.get(key);

            if (!Objects.equals(oldUserId, newUserId)) {
                if (oldUserId != null) {
                    affectedUserIds.add(oldUserId);
                }
                if (newUserId != null) {
                    affectedUserIds.add(newUserId);
                }
            }
        }

        String range = sp.getStartDate().format(PUBLISH_DATE_FMT) + " to " + sp.getEndDate().format(PUBLISH_DATE_FMT);
        String payload = "Your schedule was updated for " + range;
        String smsMessage = "SKT Scheduler: Your schedule was updated for " + range
                + ". Log in to review: " + publicLoginUrl;

        for (Long userId : affectedUserIds) {
            AppUser user = appUserRepo.findById(userId).orElse(null);
            if (user == null || !user.isEnabled()) {
                continue;
            }

            notificationDeliveryService.notifyInAppAndSms(
                    user,
                    NotificationType.SCHEDULE_UPDATED,
                    payload,
                    smsMessage
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean needsRepublish(Long schedulePeriodId) {
        var sp = periodRepo.findById(schedulePeriodId).orElseThrow();

        // If nothing was ever snapshotted, and the period is POSTED, it needs republish once.
        if ("POSTED".equalsIgnoreCase(sp.getStatus()) &&
                publishedRepo.countBySchedulePeriod_Id(sp.getId()) == 0) {
            return true;
        }

        var start = sp.getStartDate();
        var end   = sp.getEndDate();

        // Build current map: key -> userId (nullable allowed)
        record Key(LocalDate d, String per, String pos) {}
        Map<Key, Long> current = new HashMap<>();
        // load all shifts in window
        var shifts = shiftRepo.findByDateBetween(start, end);
        for (var s : shifts) {
            var aOpt = assignmentRepo.findByShift(s);
            Long uid = aOpt.map(a -> a.getEmployee() != null ? a.getEmployee().getId() : null).orElse(null);
            var k = new Key(s.getDate(), s.getPeriod().name(), s.getPosition().name());
            current.put(k, uid);
        }

        // Build published map
        Map<Key, Long> snap = new HashMap<>();
        var rows = publishedRepo.findBySchedulePeriod_IdAndDateBetween(sp.getId(), start, end);
        for (var pa : rows) {
            var k = new Key(pa.getDate(), pa.getPeriod().name(), pa.getPosition().name());
            Long uid = pa.getUser() != null ? pa.getUser().getId() : null;
            snap.put(k, uid);
        }

        // Compare both directions (detect added/removed/changed)
        if (current.size() != snap.size()) return true;
        for (var e : current.entrySet()) {
            if (!Objects.equals(e.getValue(), snap.get(e.getKey()))) return true;
        }
        return false;
    }
}
