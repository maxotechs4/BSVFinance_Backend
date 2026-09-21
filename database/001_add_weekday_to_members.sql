-- ============================================================================
-- Migration: add the "weekday" column to the members table
-- ============================================================================
-- Your application already has spring.jpa.hibernate.ddl-auto=update in
-- application.properties, so Hibernate will add this column automatically
-- the next time the backend starts — you do NOT have to run this manually
-- for the app to work.
--
-- This script is provided for anyone who wants to see/apply the exact schema
-- change explicitly in MySQL Workbench (e.g. for review, or once you switch
-- ddl-auto to 'validate' for a production deployment). Safe to re-run: it
-- only adds the column if it doesn't already exist.
-- ============================================================================

USE microfinance_db;

SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'members'
      AND COLUMN_NAME = 'weekday'
);

SET @ddl := IF(@column_exists = 0,
    'ALTER TABLE members ADD COLUMN weekday VARCHAR(10) NULL AFTER join_date',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Helpful index for the Admin page's "filter by weekday" query.
CREATE INDEX IF NOT EXISTS idx_members_weekday ON members (weekday);
