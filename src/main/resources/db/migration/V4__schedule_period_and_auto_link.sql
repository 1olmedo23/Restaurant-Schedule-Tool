/* ==========================================================
   V4 — Schedule Periods + auto-link shifts to their period
   ========================================================== */

-- 1) Create schedule_period table
CREATE TABLE IF NOT EXISTS schedule_period (
                                               id BIGSERIAL PRIMARY KEY,
                                               start_date DATE NOT NULL UNIQUE,
                                               end_date   DATE NOT NULL,
                                               status     VARCHAR(16) NOT NULL DEFAULT 'DRAFT', -- DRAFT or POSTED
    posted_at  TIMESTAMPTZ,
    posted_by_user_id BIGINT, -- store user id (nullable for now)
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_schedule_period_status CHECK (status IN ('DRAFT','POSTED')),
    CONSTRAINT ck_schedule_period_range CHECK (end_date = start_date + INTERVAL '13 days')
    );

-- 2) Add schedule_period_id to shift + FK/index
ALTER TABLE shift
    ADD COLUMN IF NOT EXISTS schedule_period_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_shift_period_id ON shift(schedule_period_id);

ALTER TABLE shift
    ADD CONSTRAINT fk_shift_schedule_period
        FOREIGN KEY (schedule_period_id) REFERENCES schedule_period(id)
            ON DELETE SET NULL;

-- 3) Backfill: create periods for existing shifts and attach them
--    date_trunc('week', <date>)::date returns the Monday of that week.
WITH distinct_periods AS (
    SELECT DISTINCT date_trunc('week', s.date)::date AS start_date
    FROM shift s
)
INSERT INTO schedule_period (start_date, end_date, status)
SELECT dp.start_date, dp.start_date + INTERVAL '13 days', 'DRAFT'
FROM distinct_periods dp
ON CONFLICT (start_date) DO NOTHING;

UPDATE shift s
SET schedule_period_id = sp.id
    FROM schedule_period sp
WHERE s.schedule_period_id IS NULL
  AND sp.start_date = date_trunc('week', s.date)::date;

-- 4) Trigger function: when a shift is inserted/its date changes,
--    ensure schedule_period exists and set schedule_period_id.
CREATE OR REPLACE FUNCTION fn_assign_schedule_period()
RETURNS TRIGGER AS $$
DECLARE
p_start DATE;
  p_id BIGINT;
BEGIN
  p_start := date_trunc('week', NEW.date)::date;

SELECT id INTO p_id FROM schedule_period WHERE start_date = p_start;
IF p_id IS NULL THEN
    INSERT INTO schedule_period (start_date, end_date, status)
    VALUES (p_start, p_start + INTERVAL '13 days', 'DRAFT')
    RETURNING id INTO p_id;
END IF;

  NEW.schedule_period_id := p_id;
RETURN NEW;
END
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_shift_period_insert ON shift;
CREATE TRIGGER trg_shift_period_insert
    BEFORE INSERT ON shift
    FOR EACH ROW
    EXECUTE FUNCTION fn_assign_schedule_period();

DROP TRIGGER IF EXISTS trg_shift_period_update ON shift;
CREATE TRIGGER trg_shift_period_update
    BEFORE UPDATE OF date ON shift
    FOR EACH ROW
    EXECUTE FUNCTION fn_assign_schedule_period();

-- 5) Touch updated_at on schedule_period updates
CREATE OR REPLACE FUNCTION fn_touch_schedule_period()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at := NOW();
RETURN NEW;
END
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_schedule_period_touch ON schedule_period;
CREATE TRIGGER trg_schedule_period_touch
    BEFORE UPDATE ON schedule_period
    FOR EACH ROW
    EXECUTE FUNCTION fn_touch_schedule_period();
