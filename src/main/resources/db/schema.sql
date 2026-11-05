CREATE TABLE IF NOT EXISTS category (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  name       TEXT NOT NULL UNIQUE,
  created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS plan (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  name        TEXT NOT NULL UNIQUE,
  description TEXT NOT NULL DEFAULT '',
  created_at  TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS task (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  name         TEXT    NOT NULL,
  description  TEXT    NOT NULL DEFAULT '',
  start_at     TEXT,
  due_at       TEXT,
  completed    INTEGER NOT NULL DEFAULT 0 CHECK (completed IN (0,1)),
  completed_at TEXT,
  category_id  INTEGER REFERENCES category(id) ON DELETE SET NULL,
  plan_id      INTEGER REFERENCES plan(id)     ON DELETE SET NULL,
  created_at   TEXT NOT NULL,
  updated_at   TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS event (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  name        TEXT NOT NULL,
  description TEXT NOT NULL DEFAULT '',
  start_at    TEXT,
  end_at      TEXT,
  created_at  TEXT NOT NULL,
  updated_at  TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_task_due       ON task(due_at);
CREATE INDEX IF NOT EXISTS idx_task_completed ON task(completed);
CREATE INDEX IF NOT EXISTS idx_task_category  ON task(category_id);
CREATE INDEX IF NOT EXISTS idx_task_plan      ON task(plan_id);
CREATE INDEX IF NOT EXISTS idx_event_end      ON event(end_at);

CREATE VIEW IF NOT EXISTS v_task AS
SELECT t.*,
       CASE
         WHEN t.completed = 1 THEN 'COMPLETED'
         WHEN t.due_at IS NOT NULL AND t.due_at < datetime('now','localtime') THEN 'FAILED'
         ELSE 'ONGOING'
       END AS status
FROM task t;

CREATE VIEW IF NOT EXISTS v_event AS
SELECT e.*,
       CASE
         WHEN e.end_at IS NOT NULL AND e.end_at <= datetime('now','localtime') THEN 'ENDED'
         WHEN e.start_at IS NOT NULL AND e.start_at > datetime('now','localtime') THEN 'NOT_STARTED'
         ELSE 'ONGOING'
       END AS state,
       CASE WHEN e.end_at IS NOT NULL AND e.end_at <= datetime('now','localtime') THEN 1 ELSE 0 END AS completed
FROM event e;