-- ============================================================
-- V2: Agile fields on TASKS + Telegram linking on USERS
-- ============================================================

-- ------------------------------------------------------------
-- USERS: store Telegram user ID for bot authentication
-- ------------------------------------------------------------
ALTER TABLE users ADD (telegram_id NUMBER);
CREATE UNIQUE INDEX idx_users_telegram_id ON users (telegram_id);

-- ------------------------------------------------------------
-- TASKS: agile estimation and definition-of-done fields
--
--   estimated_hours  max 4 h per task (enforced in bot logic)
--   actual_hours     recorded at completion via /completetask
--   story_points     velocity metric used in sprint board
--   acceptance_criteria  definition of done (free text)
-- ------------------------------------------------------------
ALTER TABLE tasks ADD (
    estimated_hours     NUMBER(4,1),
    actual_hours        NUMBER(4,1),
    story_points        NUMBER(3),
    acceptance_criteria CLOB
);
