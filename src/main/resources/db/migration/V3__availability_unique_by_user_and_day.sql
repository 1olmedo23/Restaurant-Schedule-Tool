/* ==========================================================
   V3 — Availability uniqueness per (user_id, day_of_week)
   ========================================================== */

-- Drop the old unique on user_id (added in V2)
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'uq_availability_user'
  ) THEN
    EXECUTE 'ALTER TABLE availability DROP CONSTRAINT uq_availability_user';
END IF;
END
$$ LANGUAGE plpgsql;

-- Add the correct unique across (user_id, day_of_week)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'uq_availability_user_day'
  ) THEN
    EXECUTE 'ALTER TABLE availability
             ADD CONSTRAINT uq_availability_user_day
             UNIQUE (user_id, day_of_week)';
END IF;
END
$$ LANGUAGE plpgsql;

-- Refresh supporting index: prefer a composite index that matches lookups
DROP INDEX IF EXISTS idx_availability_user_id;
CREATE INDEX IF NOT EXISTS idx_availability_user_day
    ON availability(user_id, day_of_week);
