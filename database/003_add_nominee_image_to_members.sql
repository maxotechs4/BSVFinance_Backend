-- ============================================================================
-- Migration: add nominee photo columns to members
-- ============================================================================
-- Your application already has spring.jpa.hibernate.ddl-auto=update in
-- application.properties, so Hibernate will add these columns automatically
-- the next time the backend starts — you do NOT have to run this manually
-- for the app to work.
--
-- This script is provided for anyone who wants to see/apply the exact schema
-- change explicitly (e.g. for review, or once you switch ddl-auto to
-- 'validate' for a production deployment). Safe to re-run: it only adds a
-- column if it doesn't already exist.
--
-- Adds three columns used by the Member Profile page's "Upload Image" /
-- "View Image" buttons for the nominee's photo:
--   - nominee_image_data:          the raw image bytes (LONGBLOB / bytea)
--   - nominee_image_content_type:  e.g. "image/jpeg", "image/png"
--   - nominee_image_file_name:     original uploaded file name
-- All three are nullable — existing members simply have no nominee image
-- until one is uploaded from the profile page.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- MySQL variant (uncomment if your deployment uses MySQL)
-- ---------------------------------------------------------------------------
-- USE microfinance_db;
--
-- SET @col_exists = (
--     SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
--     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'members'
--       AND COLUMN_NAME = 'nominee_image_data'
-- );
-- SET @ddl := IF(@col_exists = 0,
--     'ALTER TABLE members ADD COLUMN nominee_image_data LONGBLOB NULL AFTER nominee_voter_id',
--     'SELECT 1');
-- PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
--
-- SET @col_exists = (
--     SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
--     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'members'
--       AND COLUMN_NAME = 'nominee_image_content_type'
-- );
-- SET @ddl := IF(@col_exists = 0,
--     'ALTER TABLE members ADD COLUMN nominee_image_content_type VARCHAR(100) NULL AFTER nominee_image_data',
--     'SELECT 1');
-- PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
--
-- SET @col_exists = (
--     SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
--     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'members'
--       AND COLUMN_NAME = 'nominee_image_file_name'
-- );
-- SET @ddl := IF(@col_exists = 0,
--     'ALTER TABLE members ADD COLUMN nominee_image_file_name VARCHAR(255) NULL AFTER nominee_image_content_type',
--     'SELECT 1');
-- PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- PostgreSQL variant (matches the active spring.datasource.url in
-- application.properties)
-- ---------------------------------------------------------------------------
ALTER TABLE members ADD COLUMN IF NOT EXISTS nominee_image_data BYTEA;
ALTER TABLE members ADD COLUMN IF NOT EXISTS nominee_image_content_type VARCHAR(100);
ALTER TABLE members ADD COLUMN IF NOT EXISTS nominee_image_file_name VARCHAR(255);

-- ---------------------------------------------------------------------------
-- FIX for anyone who already started the backend once with the earlier
-- version of this code that had @Lob on nominee_image_data: Hibernate would
-- have created that column as "oid" (Large Object reference) instead of
-- "bytea", which then fails at runtime with a type-mismatch error the first
-- time an image is uploaded. Run this once to correct it (safe even if the
-- column is already bytea — the USING clause just no-ops in that case).
-- If the column was created as oid and already has (broken) data in it, this
-- drops that column and recreates it empty, since nothing usable could have
-- been stored in it anyway.
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'members' AND column_name = 'nominee_image_data' AND data_type = 'oid'
    ) THEN
        ALTER TABLE members DROP COLUMN nominee_image_data;
        ALTER TABLE members ADD COLUMN nominee_image_data BYTEA;
    END IF;
END $$;
