-- Tracks lightweight edits to posted schedules: one row per (period, date, role)
-- We persist these across republishes so managers can always see what changed.

CREATE TABLE amendment (
                           id BIGSERIAL PRIMARY KEY,

                           schedule_period_id BIGINT NOT NULL
                               REFERENCES schedule_period(id) ON DELETE CASCADE,

                           date DATE NOT NULL,                 -- Which calendar day (shift.date)
                           period VARCHAR(16) NOT NULL,        -- ShiftPeriod enum name: LUNCH / DINNER
                           position VARCHAR(32) NOT NULL,      -- Position enum name (e.g., SERVER_1, EXPO, ...)

                           original_employee_id BIGINT NULL
        REFERENCES app_user(id) ON DELETE SET NULL,

                           new_employee_id BIGINT NULL
        REFERENCES app_user(id) ON DELETE SET NULL,

                           changed_by BIGINT NULL
        REFERENCES app_user(id) ON DELETE SET NULL,

                           changed_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- One row per (period, date, role) inside a given posted schedule period
                           CONSTRAINT uq_amendment UNIQUE (schedule_period_id, date, period, position)
);

-- Helpful indexes for your window queries and period lookups
CREATE INDEX idx_amendment_date           ON amendment(date);
CREATE INDEX idx_amendment_period_fk      ON amendment(schedule_period_id);
