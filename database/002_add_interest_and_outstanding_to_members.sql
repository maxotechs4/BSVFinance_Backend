-- ============================================================================
-- Migration: add "interest_percentage" and "outstanding_amount" to members
-- ============================================================================
-- Your application already has spring.jpa.hibernate.ddl-auto=update in
-- application.properties, so Hibernate will add these columns automatically
-- the next time the backend starts — you do NOT have to run this manually
-- for the app to work.
--
-- This script is provided for anyone who wants to see/apply the exact schema
-- change explicitly in MySQL Workbench (e.g. for review, or once you switch
-- ddl-auto to 'validate' for a production deployment). Safe to re-run: it
-- only adds a column if it doesn't already exist.
--
-- NOTE: existing members will have NULL in both new columns right after this
-- runs (this migration does not back-fill historical data). The backend
-- already handles that gracefully — it treats a NULL interest_percentage as
-- 0% and computes outstanding_amount on the fly whenever it's NULL, so
-- nothing breaks. The optional backfill query at the bottom sets sensible
-- starting values for existing rows if you'd rather have them stored
-- immediately instead of computed on read.
-- ============================================================================

USE microfinance_db;

SET @interest_col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'members'
      AND COLUMN_NAME = 'interest_percentage'
);

SET @ddl := IF(@interest_col_exists = 0,
    'ALTER TABLE members ADD COLUMN interest_percentage DECIMAL(5,2) NULL DEFAULT 0 AFTER loan_amount',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @outstanding_col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'members'
      AND COLUMN_NAME = 'outstanding_amount'
);

SET @ddl := IF(@outstanding_col_exists = 0,
    'ALTER TABLE members ADD COLUMN outstanding_amount DECIMAL(12,2) NULL AFTER interest_percentage',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ----------------------------------------------------------------------------
-- OPTIONAL one-time backfill for existing members: sets interest_percentage
-- to 0 and outstanding_amount to the loan amount minus principal collected
-- so far, assuming 0% interest historically (i.e. every rupee paid reduced
-- principal). If you've been tracking interest separately, adjust the
-- interest_percentage per member first before running the outstanding_amount
-- update, or simply skip this block — the backend computes the correct
-- figure on the fly for any member whose outstanding_amount is still NULL.
-- ----------------------------------------------------------------------------

-- UPDATE members SET interest_percentage = 0 WHERE interest_percentage IS NULL;
--
-- UPDATE members m
-- SET m.outstanding_amount = GREATEST(
--     COALESCE(m.loan_amount, 0) - COALESCE((
--         SELECT SUM(p.amount_paid) FROM payments p WHERE p.member_id = m.id
--     ), 0),
--     0
-- )
-- WHERE m.outstanding_amount IS NULL;
