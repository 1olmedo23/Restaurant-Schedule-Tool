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

    public PublishServiceImpl(SchedulePeriodRepository periodRepo,
                              ShiftRepository shiftRepo,
                              AssignmentRepository assignmentRepo,
                              PublishedAssignmentRepository publishedRepo) {
        this.periodRepo = periodRepo;
        this.shiftRepo = shiftRepo;
        this.assignmentRepo = assignmentRepo;
        this.publishedRepo = publishedRepo;
    }

    @Override
    @Transactional
    public void snapshotPeriod(Long schedulePeriodId) {
        SchedulePeriod sp = periodRepo.findById(schedulePeriodId).orElseThrow();
        if (!"POSTED".equalsIgnoreCase(sp.getStatus())) return;

        // wipe old snapshot rows for THIS period
        publishedRepo.deleteAllBySchedulePeriodId(sp.getId());

        // …then rebuild
        var shifts = shiftRepo.findByDateBetween(sp.getStartDate(), sp.getEndDate());
        for (var s : shifts) {
            var aOpt = assignmentRepo.findByShift(s);
            if (aOpt.isEmpty()) continue;

            var a = aOpt.get();
            var pub = new PublishedAssignment();
            pub.setSchedulePeriod(sp);
            pub.setDate(s.getDate());
            pub.setPeriod(s.getPeriod());
            pub.setPosition(s.getPosition());
            pub.setUser(a.getEmployee()); // can be null
            publishedRepo.save(pub);
        }
    }

    @Override
    @Transactional
    public void snapshotLatestPosted() {
        periodRepo.findTopByStatusOrderByStartDateDesc("POSTED")
                .ifPresent(sp -> snapshotPeriod(sp.getId()));
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
