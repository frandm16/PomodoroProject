CREATE TABLE IF NOT EXISTS day_note (
                                        id BIGSERIAL PRIMARY KEY,
                                        date      DATE        NOT NULL UNIQUE,
                                        content   TEXT        NOT NULL DEFAULT ''
);

CREATE TABLE IF NOT EXISTS todo_item (
                                         id        BIGSERIAL PRIMARY KEY,
                                         date      DATE        NOT NULL,
                                         text      VARCHAR(500) NOT NULL,
    completed BOOLEAN     NOT NULL DEFAULT FALSE,
    position  INTEGER     NOT NULL DEFAULT 0
    );
