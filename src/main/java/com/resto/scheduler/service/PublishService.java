package com.resto.scheduler.service;

public interface PublishService {
    /** Rebuild the published snapshot for the given POSTED schedule period id. */
    void snapshotPeriod(Long schedulePeriodId);

    /** Rebuild the snapshot for the latest POSTED period (no-op if none). */
    void snapshotLatestPosted();

    boolean needsRepublish(Long schedulePeriodId);
}
