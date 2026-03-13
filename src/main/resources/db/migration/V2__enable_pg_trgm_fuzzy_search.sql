-- Enable PostgreSQL trigram extension for fuzzy / similarity search.
-- pg_trgm breaks strings into 3-character sequences ("trigrams") and measures
-- how many trigrams two strings share.  This lets us match "Grocery" ≈ "Groceries".
--
-- Requires superuser or the pg_extension privilege (available on most hosted PG instances).
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- GIN index on the title column accelerates similarity searches dramatically.
-- Without it, every fuzzy query would require a full table scan.
CREATE INDEX IF NOT EXISTS idx_tasks_title_trgm ON tasks USING GIN (title gin_trgm_ops);

