ALTER TABLE sop
    ADD COLUMN sop_description TEXT;

UPDATE sop
SET sop_description = 'N/A'
WHERE sop_description IS NULL;

ALTER TABLE sop
    MODIFY sop_description TEXT NOT NULL;

ALTER TABLE sop
    DROP COLUMN sop_location_path;

