-- Split into individual statements for maximum compatibility
ALTER TABLE tasks ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tasks ADD COLUMN last_modified_at TIMESTAMP;
ALTER TABLE tasks ADD COLUMN deleted_at TIMESTAMP;