ALTER TABLE sop
    ADD COLUMN supersedes_sop_id INT NULL,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE sop
    ADD CONSTRAINT fk_sop_supersedes
    FOREIGN KEY (supersedes_sop_id) REFERENCES sop(sop_id);

ALTER TABLE change_request
    ADD COLUMN original_sop_id INT,
    ADD COLUMN proposed_sop_id INT;

ALTER TABLE change_request
    ADD CONSTRAINT fk_cr_original_sop_id
    FOREIGN KEY (original_sop_id) REFERENCES sop(sop_id);

ALTER TABLE change_request
    ADD CONSTRAINT fk_cr_proposed_sop_id
        FOREIGN KEY (proposed_sop_id) REFERENCES sop(sop_id);

/*FLIP sop_id*/
UPDATE change_request
    SET original_sop_id = sop_id,
        proposed_sop_id = sop_id;

ALTER TABLE change_request
    DROP COLUMN sop_id;

ALTER TABLE change_request
    MODIFY COLUMN original_sop_id INT NOT NULL,
    MODIFY COLUMN proposed_sop_id INT NOT NULL;