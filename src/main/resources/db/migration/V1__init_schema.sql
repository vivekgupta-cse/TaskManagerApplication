CREATE TABLE tasks (
                       id BIGSERIAL PRIMARY KEY,
                       title VARCHAR(100) NOT NULL,
                       description VARCHAR(500),
                       completed BOOLEAN DEFAULT FALSE NOT NULL
);

-- Adding a unique constraint to title as a security improvement
-- This prevents duplicates at the database level
ALTER TABLE tasks ADD CONSTRAINT uc_task_title UNIQUE (title);