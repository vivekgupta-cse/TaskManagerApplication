-- Add a column to track if a task is deleted
ALTER TABLE tasks ADD COLUMN deleted BOOLEAN DEFAULT FALSE NOT NULL;

-- Index for performance, as almost every query will now filter by this column
CREATE INDEX idx_tasks_deleted ON tasks(deleted);