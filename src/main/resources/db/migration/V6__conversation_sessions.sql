-- ============================================================
-- TABLE: CONVERSATION_SESSIONS
-- Stores per-user Telegram bot session context and AI conversation
-- history. Survives server restarts. One row per Telegram user.
-- user_id is nullable: populated once the user links their Telegram
-- account; sessions for unlinked users are still fully functional.
-- ============================================================
CREATE TABLE conversation_sessions (
    telegram_user_id    NUMBER           NOT NULL,
    user_id             NUMBER,
    active_project_id   NUMBER,
    active_project_name VARCHAR2(255),
    active_cap_id       NUMBER,
    active_cap_name     VARCHAR2(255),
    active_feature_id   NUMBER,
    active_feature_name VARCHAR2(255),
    active_story_id     NUMBER,
    active_story_title  VARCHAR2(500),
    active_task_id      NUMBER,
    active_task_title   VARCHAR2(500),
    history_json        CLOB,
    last_activity       TIMESTAMP        DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_conv_sessions         PRIMARY KEY (telegram_user_id),
    CONSTRAINT fk_conv_sessions_user    FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_conv_history_json    CHECK (history_json IS NULL OR LENGTH(history_json) > 0)
);

CREATE INDEX idx_conv_sessions_user_id ON conversation_sessions (user_id);
