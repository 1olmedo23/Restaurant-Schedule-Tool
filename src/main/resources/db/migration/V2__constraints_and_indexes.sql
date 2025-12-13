/* ==========================================================
   V2 — Constraints, Indexes, and Preflight De-duplication
   (Updated to match current schema: availability is per user+day,
    shift.position is quoted)
   ========================================================== */

-- --------- A) AVAILABILITY: keep exactly one row per (user_id, day_of_week)
DELETE FROM availability a
    USING availability b
WHERE a.user_id = b.user_id
  AND a.day_of_week = b.day_of_week
  AND a.ctid > b.ctid;

-- --------- B) USER_ROLES: remove duplicate (user_id, role_id)
DELETE FROM user_roles ur
    USING user_roles ur2
WHERE ur.user_id = ur2.user_id
  AND ur.role_id = ur2.role_id
  AND ur.ctid > ur2.ctid;

-- --------- C) SHIFT: merge duplicate (date, period, "position")
CREATE TEMP TABLE t_shift_dups AS
SELECT date, period, "position", MIN(id) AS keep_id
FROM shift
GROUP BY date, period, "position"
HAVING COUNT(*) > 1;

CREATE TEMP TABLE t_shift_to_move AS
SELECT s.id AS id, d.keep_id
FROM shift s
         JOIN t_shift_dups d
              ON s.date = d.date
                  AND s.period = d.period
                  AND s."position" = d."position";

UPDATE assignment a
SET shift_id = t.keep_id
    FROM t_shift_to_move t
WHERE a.shift_id = t.id
  AND t.id <> t.keep_id;

DELETE FROM assignment a
    USING assignment b
WHERE a.shift_id = b.shift_id
  AND a.ctid > b.ctid;

DELETE FROM shift s
    USING t_shift_to_move t
WHERE s.id = t.id
  AND s.id <> t.keep_id;

DROP TABLE t_shift_to_move;
DROP TABLE t_shift_dups;

-- ========= APP USER (uniques)
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_app_user_username') THEN
    EXECUTE 'ALTER TABLE app_user ADD CONSTRAINT uq_app_user_username UNIQUE (username)';
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_app_user_email') THEN
    EXECUTE 'ALTER TABLE app_user ADD CONSTRAINT uq_app_user_email UNIQUE (email)';
END IF;
END
$$ LANGUAGE plpgsql;

-- ========= ROLE / USER_ROLES
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_user_roles') THEN
    EXECUTE 'ALTER TABLE user_roles ADD CONSTRAINT uq_user_roles UNIQUE (user_id, role_id)';
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_roles_user') THEN
    EXECUTE 'ALTER TABLE user_roles
             ADD CONSTRAINT fk_user_roles_user
             FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE';
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_roles_role') THEN
    EXECUTE 'ALTER TABLE user_roles
             ADD CONSTRAINT fk_user_roles_role
             FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE';
END IF;
END
$$ LANGUAGE plpgsql;

CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles(role_id);

-- ========= AVAILABILITY (FK only here; unique is handled by your V3 migration)
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_availability_user') THEN
    EXECUTE 'ALTER TABLE availability
             ADD CONSTRAINT fk_availability_user
             FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE';
END IF;
END
$$ LANGUAGE plpgsql;

CREATE INDEX IF NOT EXISTS idx_availability_user_day ON availability(user_id, day_of_week);

-- ========= SHIFT (unique index on (date, period, "position"))
CREATE UNIQUE INDEX IF NOT EXISTS uq_shift_dpp ON shift(date, period, "position");
CREATE INDEX IF NOT EXISTS idx_shift_date ON shift(date);

-- ========= ASSIGNMENT (unique + FKs)
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_assignment_shift') THEN
    EXECUTE 'ALTER TABLE assignment ADD CONSTRAINT uq_assignment_shift UNIQUE (shift_id)';
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_assignment_user') THEN
    EXECUTE 'ALTER TABLE assignment
             ADD CONSTRAINT fk_assignment_user
             FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE';
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_assignment_shift') THEN
    EXECUTE 'ALTER TABLE assignment
             ADD CONSTRAINT fk_assignment_shift
             FOREIGN KEY (shift_id) REFERENCES shift(id) ON DELETE CASCADE';
END IF;
END
$$ LANGUAGE plpgsql;

CREATE INDEX IF NOT EXISTS idx_assignment_user_id ON assignment(user_id);
CREATE INDEX IF NOT EXISTS idx_assignment_shift_id ON assignment(shift_id);
