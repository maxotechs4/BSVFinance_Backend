-- ============================================================================
-- Migration: multi-image nominee photo support
-- ============================================================================
-- Your application already has spring.jpa.hibernate.ddl-auto=update in
-- application.properties, so Hibernate will create the new nominee_images
-- table automatically the next time the backend starts — you do NOT have to
-- run this manually for the app to work. Hibernate will NOT drop the old
-- nominee_image_data/nominee_image_content_type/nominee_image_file_name
-- columns on members though (ddl-auto=update never drops columns), so run
-- this once to migrate any existing single photo across and clean those up.
--
-- Safe to re-run: the table/column creation steps only act if not already
-- present, and the data migration step only copies rows that haven't been
-- migrated yet (guarded by the NOT EXISTS check).
-- ============================================================================

-- ---------------------------------------------------------------------------
-- PostgreSQL variant (matches the active spring.datasource.url in
-- application.properties)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS nominee_images (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id) ON DELETE CASCADE,
    image_data BYTEA NOT NULL,
    content_type VARCHAR(100),
    file_name VARCHAR(255),
    uploaded_at TIMESTAMP
);

-- Carry over each member's single old photo (if any) as the first row in the
-- new table, only if it hasn't already been migrated.
INSERT INTO nominee_images (member_id, image_data, content_type, file_name, uploaded_at)
SELECT m.id, m.nominee_image_data, m.nominee_image_content_type, m.nominee_image_file_name, m.updated_at
FROM members m
WHERE m.nominee_image_data IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM nominee_images n WHERE n.member_id = m.id);

-- Old single-image columns are superseded by the nominee_images table above.
ALTER TABLE members DROP COLUMN IF EXISTS nominee_image_data;
ALTER TABLE members DROP COLUMN IF EXISTS nominee_image_content_type;
ALTER TABLE members DROP COLUMN IF EXISTS nominee_image_file_name;

-- ---------------------------------------------------------------------------
-- MySQL variant (uncomment if your deployment uses MySQL)
-- ---------------------------------------------------------------------------
-- USE microfinance_db;
--
-- CREATE TABLE IF NOT EXISTS nominee_images (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     member_id BIGINT NOT NULL,
--     image_data LONGBLOB NOT NULL,
--     content_type VARCHAR(100),
--     file_name VARCHAR(255),
--     uploaded_at DATETIME,
--     CONSTRAINT fk_nominee_images_member FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE
-- );
--
-- INSERT INTO nominee_images (member_id, image_data, content_type, file_name, uploaded_at)
-- SELECT m.id, m.nominee_image_data, m.nominee_image_content_type, m.nominee_image_file_name, m.updated_at
-- FROM members m
-- WHERE m.nominee_image_data IS NOT NULL
--   AND NOT EXISTS (SELECT 1 FROM nominee_images n WHERE n.member_id = m.id);
--
-- ALTER TABLE members DROP COLUMN nominee_image_data;
-- ALTER TABLE members DROP COLUMN nominee_image_content_type;
-- ALTER TABLE members DROP COLUMN nominee_image_file_name;
