CREATE TABLE tags (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    color VARCHAR(20) DEFAULT '#ffffff',
    is_archived BOOLEAN DEFAULT false,
    weekly_goal_min INTEGER DEFAULT 0
);

CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    tag_id BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    is_favorite BOOLEAN DEFAULT false,
    weekly_goal_min INTEGER DEFAULT 0,
    UNIQUE(tag_id, name)
);

CREATE TABLE sessions (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    title TEXT,
    description TEXT,
    total_minutes INTEGER NOT NULL,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    rating INTEGER DEFAULT 0,
    is_favorite BOOLEAN DEFAULT false
);

CREATE TABLE scheduled_sessions (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    title TEXT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    is_completed BOOLEAN DEFAULT false
);

CREATE TABLE deadlines (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    tag_id BIGINT REFERENCES tags(id) ON DELETE SET NULL,
    task_id BIGINT REFERENCES tasks(id) ON DELETE SET NULL,
    urgency VARCHAR(10) DEFAULT 'MEDIUM',
    due_date TIMESTAMP NOT NULL,
    all_day BOOLEAN DEFAULT FALSE,
    description TEXT,
    is_completed BOOLEAN DEFAULT FALSE
);
