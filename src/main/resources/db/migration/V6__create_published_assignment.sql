-- Stores what employees actually see (only refreshed on Publish/Republish)
CREATE TABLE IF NOT EXISTS published_assignment (
                                                    id                  BIGSERIAL PRIMARY KEY,
                                                    schedule_period_id  BIGINT NOT NULL REFERENCES schedule_period(id) ON DELETE CASCADE,
    date                DATE   NOT NULL,
    period              VARCHAR(16) NOT NULL,   -- LUNCH | DINNER
    position            VARCHAR(32) NOT NULL,   -- SERVER_1, EXPO, etc.
    user_id             BIGINT REFERENCES app_user(id) ON DELETE SET NULL,

    CONSTRAINT uq_published UNIQUE (schedule_period_id, date, period, position)
    );

CREATE INDEX IF NOT EXISTS idx_pub_assign_period ON published_assignment(schedule_period_id);
CREATE INDEX IF NOT EXISTS idx_pub_assign_date   ON published_assignment(date);
