-- V4: Ensure agile columns exist on TASKS.
-- V2's multi-column ALTER TABLE ADD is silently ignored by Oracle Autonomous
-- Database when the statement contains a LOB column; none of the columns in
-- that batch were materialised. Each column is added here individually, and
-- ORA-01430 (column already exists) is swallowed so the migration is
-- idempotent regardless of the current DB state.

DECLARE
  PROCEDURE add_col(p_ddl IN VARCHAR2) IS
  BEGIN
    EXECUTE IMMEDIATE p_ddl;
  EXCEPTION
    WHEN OTHERS THEN
      IF SQLCODE = -1430 THEN NULL; -- column already exists, skip
      ELSE RAISE;
      END IF;
  END;
BEGIN
  add_col('ALTER TABLE tasks ADD (estimated_hours NUMBER(4,1))');
  add_col('ALTER TABLE tasks ADD (actual_hours     NUMBER(4,1))');
  add_col('ALTER TABLE tasks ADD (story_points     NUMBER(3))');
  add_col('ALTER TABLE tasks ADD (acceptance_criteria VARCHAR2(4000))');
END;
/
