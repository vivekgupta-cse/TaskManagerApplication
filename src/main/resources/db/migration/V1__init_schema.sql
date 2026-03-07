-- 1. Create Users Table first (in case of future Foreign Key dependencies)
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(255) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       role VARCHAR(50) NOT NULL
);

-- 2. Create Tasks Table with all constraints and columns
CREATE TABLE tasks (
                       id BIGSERIAL PRIMARY KEY,
                       title VARCHAR(100) NOT NULL,
                       description VARCHAR(500),
                       completed BOOLEAN DEFAULT FALSE NOT NULL,
                       deleted BOOLEAN DEFAULT FALSE NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       last_modified_at TIMESTAMP,
                       deleted_at TIMESTAMP,

    -- Named unique constraint for better error handling/identification
                       CONSTRAINT uc_task_title UNIQUE (title)
);

-- 3. Optimization: Partial Index for Soft Deletes
-- Instead of indexing all rows, we index only those not deleted.
-- This keeps the index small and fast for the most common queries.
CREATE INDEX idx_tasks_active ON tasks(deleted) WHERE deleted = FALSE;