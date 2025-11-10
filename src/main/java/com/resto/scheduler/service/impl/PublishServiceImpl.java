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

import java.util.List;

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
}
