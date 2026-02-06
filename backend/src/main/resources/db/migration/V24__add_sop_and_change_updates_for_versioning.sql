/*ALTER TABLE sop
    ADD COLUMN supersedes_sop_id INT NULL;

ALTER TABLE sop
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';*/

SET @fk := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sop'
      AND CONSTRAINT_NAME = 'fk_sop_supersedes'
);

SET @sql := IF(@fk = 0,
               'ALTER TABLE sop ADD CONSTRAINT fk_sop_supersedes FOREIGN KEY (supersedes_sop_id) REFERENCES sop(sop_id)',
               'SELECT 1');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

/*ALTER TABLE change_request
    ADD COLUMN original_sop_id INT,
    ADD COLUMN proposed_sop_id INT;*/

UPDATE change_request
SET original_sop_id = sop_id,
    proposed_sop_id = sop_id
WHERE original_sop_id IS NULL
   OR proposed_sop_id IS NULL;

SET @fk_old := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'change_request'
      AND CONSTRAINT_NAME = 'fk_change_request_sop'
);

SET @sql2 := IF(@fk_old = 1,
                'ALTER TABLE change_request DROP FOREIGN KEY fk_change_request_sop',
                'SELECT 1');

PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

SET @col := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'change_request'
      AND COLUMN_NAME = 'sop_id'
);

SET @sql3 := IF(@col = 1,
                'ALTER TABLE change_request DROP COLUMN sop_id',
                'SELECT 1');

PREPARE stmt3 FROM @sql3;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

SET @fk1 := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'change_request'
      AND CONSTRAINT_NAME = 'fk_cr_original_sop_id'
);

SET @sql4 := IF(@fk1 = 0,
                'ALTER TABLE change_request ADD CONSTRAINT fk_cr_original_sop_id FOREIGN KEY (original_sop_id) REFERENCES sop(sop_id)',
                'SELECT 1');

PREPARE stmt4 FROM @sql4;
EXECUTE stmt4;
DEALLOCATE PREPARE stmt4;

SET @fk2 := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'change_request'
      AND CONSTRAINT_NAME = 'fk_cr_proposed_sop_id'
);

SET @sql5 := IF(@fk2 = 0,
                'ALTER TABLE change_request ADD CONSTRAINT fk_cr_proposed_sop_id FOREIGN KEY (proposed_sop_id) REFERENCES sop(sop_id)',
                'SELECT 1');

PREPARE stmt5 FROM @sql5;
EXECUTE stmt5;
DEALLOCATE PREPARE stmt5;

ALTER TABLE change_request
    MODIFY COLUMN original_sop_id INT NOT NULL,
    MODIFY COLUMN proposed_sop_id INT NOT NULL;

