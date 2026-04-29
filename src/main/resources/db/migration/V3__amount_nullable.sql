-- MIGRATION transactions carry no cash amount (position transfer only)
ALTER TABLE transactions ALTER COLUMN amount DROP NOT NULL;
