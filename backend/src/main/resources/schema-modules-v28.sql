-- ══════════════════════════════════════════════════════════════════════════
--  ReDiscoverU v28 — New Modules Database Schema
--  Run ONLY if upgrading from an existing installation.
--  Fresh installs: no action needed — Hibernate ddl-auto=update handles it.
-- ══════════════════════════════════════════════════════════════════════════

-- ── MODULE 1: Learning Progress (already exists — included for reference) ─
CREATE TABLE IF NOT EXISTS lesson_progress (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    program_id      BIGINT NOT NULL,
    content_id      BIGINT NOT NULL,
    content_type    VARCHAR(20) NOT NULL,
    content_title   VARCHAR(300),
    status          ENUM('NOT_STARTED','IN_PROGRESS','COMPLETED') NOT NULL DEFAULT 'NOT_STARTED',
    watched_seconds INT DEFAULT 0,
    total_seconds   INT DEFAULT 0,
    completion_pct  INT DEFAULT 0,
    last_accessed_at DATETIME,
    completed_at    DATETIME,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_lesson_progress UNIQUE (user_id, content_id, content_type),
    CONSTRAINT fk_lp_user    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT fk_lp_program FOREIGN KEY (program_id) REFERENCES programs(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_lp_user_prog ON lesson_progress(user_id, program_id);

CREATE TABLE IF NOT EXISTS program_progress (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id           BIGINT NOT NULL,
    program_id        BIGINT NOT NULL,
    overall_pct       INT NOT NULL DEFAULT 0,
    total_lessons     INT NOT NULL DEFAULT 0,
    completed_lessons INT NOT NULL DEFAULT 0,
    last_content_id   BIGINT,
    last_content_type VARCHAR(20),
    last_content_title VARCHAR(300),
    started_at        DATETIME,
    completed_at      DATETIME,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_program_progress UNIQUE (user_id, program_id),
    CONSTRAINT fk_pp_user    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT fk_pp_program FOREIGN KEY (program_id) REFERENCES programs(id) ON DELETE CASCADE
);

-- ── MODULE 2: Notes & Bookmarks ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_notes (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id             BIGINT NOT NULL,
    program_id          BIGINT NOT NULL,
    content_id          BIGINT NOT NULL,
    content_type        VARCHAR(20) NOT NULL,
    content_title       VARCHAR(300),
    timestamp_seconds   INT,
    note_text           TEXT NOT NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_note_user    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT fk_note_program FOREIGN KEY (program_id) REFERENCES programs(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_notes_user_program ON user_notes(user_id, program_id);
CREATE INDEX IF NOT EXISTS idx_notes_content      ON user_notes(user_id, content_id, content_type);

CREATE TABLE IF NOT EXISTS bookmarks (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id             BIGINT NOT NULL,
    program_id          BIGINT NOT NULL,
    content_id          BIGINT NOT NULL,
    content_type        VARCHAR(20) NOT NULL,
    content_title       VARCHAR(300),
    timestamp_seconds   INT,
    label               VARCHAR(200),
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bm_user    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT fk_bm_program FOREIGN KEY (program_id) REFERENCES programs(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_bm_user_program ON bookmarks(user_id, program_id);
CREATE INDEX IF NOT EXISTS idx_bm_content      ON bookmarks(user_id, content_id, content_type);

-- ── MODULE 3: In-App Notifications ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS in_app_notifications (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    type        ENUM('ANNOUNCEMENT','NEW_SESSION','NEW_CONTENT','BADGE_EARNED','COMMENT_REPLY','SYSTEM')
                NOT NULL DEFAULT 'SYSTEM',
    title       VARCHAR(200) NOT NULL,
    message     TEXT,
    link        VARCHAR(300),
    is_read     TINYINT(1) NOT NULL DEFAULT 0,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at     DATETIME,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_notif_user_read ON in_app_notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notif_user_date ON in_app_notifications(user_id, created_at);

-- ── MODULE 5: Gamification ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_streaks (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id             BIGINT NOT NULL UNIQUE,
    current_streak      INT NOT NULL DEFAULT 0,
    longest_streak      INT NOT NULL DEFAULT 0,
    total_active_days   INT NOT NULL DEFAULT 0,
    last_activity_date  DATE,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_streak_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS badges (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    name             VARCHAR(100) NOT NULL UNIQUE,
    description      TEXT,
    icon_url         VARCHAR(500),
    badge_type       ENUM('FIRST_LESSON','LESSONS_COMPLETED','PROGRAM_COMPLETE',
                          'STREAK_DAYS','NOTE_TAKER','BOOKMARKS_ADDED','COMMUNITY_ACTIVE')
                     NOT NULL,
    threshold_value  INT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS user_badges (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    badge_id    BIGINT NOT NULL,
    earned_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_badge  UNIQUE (user_id, badge_id),
    CONSTRAINT fk_ub_user     FOREIGN KEY (user_id)  REFERENCES users(id)  ON DELETE CASCADE,
    CONSTRAINT fk_ub_badge    FOREIGN KEY (badge_id) REFERENCES badges(id) ON DELETE CASCADE
);

-- ── MODULE 6: Discussion / Community ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS comments (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id       BIGINT NOT NULL,
    program_id    BIGINT NOT NULL,
    content_id    BIGINT NOT NULL,
    content_type  VARCHAR(20) NOT NULL,
    comment_text  TEXT NOT NULL,
    parent_id     BIGINT,
    like_count    INT NOT NULL DEFAULT 0,
    reply_count   INT NOT NULL DEFAULT 0,
    deleted       TINYINT(1) NOT NULL DEFAULT 0,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME,
    CONSTRAINT fk_comment_user    FOREIGN KEY (user_id)   REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT fk_comment_program FOREIGN KEY (program_id) REFERENCES programs(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_comment_content ON comments(program_id, content_id, content_type);
CREATE INDEX IF NOT EXISTS idx_comment_parent  ON comments(parent_id);

CREATE TABLE IF NOT EXISTS comment_likes (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    comment_id  BIGINT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_comment_like UNIQUE (user_id, comment_id),
    CONSTRAINT fk_cl_user      FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT fk_cl_comment   FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE
);

-- ── Default Badges (seed data) ───────────────────────────────────────────
INSERT IGNORE INTO badges (name, description, icon_url, badge_type, threshold_value) VALUES
  ('First Step',       'Complete your very first lesson',       '🎯', 'FIRST_LESSON',      1),
  ('5 Lessons Done',   'Complete 5 lessons',                    '📚', 'LESSONS_COMPLETED', 5),
  ('10 Lessons Done',  'Complete 10 lessons',                   '🏆', 'LESSONS_COMPLETED', 10),
  ('25 Lessons Done',  'Complete 25 lessons',                   '🌟', 'LESSONS_COMPLETED', 25),
  ('Program Graduate', 'Complete an entire program',            '🎓', 'PROGRAM_COMPLETE',  1),
  ('3-Day Streak',     'Learn 3 days in a row',                 '🔥', 'STREAK_DAYS',       3),
  ('7-Day Streak',     'Learn 7 days in a row — one week!',     '⚡', 'STREAK_DAYS',       7),
  ('30-Day Streak',    'Learn 30 days in a row!',               '💎', 'STREAK_DAYS',       30);
