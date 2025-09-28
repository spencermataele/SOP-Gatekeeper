-- Relax column to allow cleanup (V1 made it NOT NULL)
ALTER TABLE sop
    MODIFY COLUMN process_id INT NULL;

-- Add FK (use a fresh name so we never collide with a prior attempt)
-- If a process gets deleted, keep the SOP by nulling out the link.
ALTER TABLE sop
    ADD CONSTRAINT fk_sop_business_process_id
        FOREIGN KEY (process_id) REFERENCES business_process(process_id)
            ON DELETE SET NULL
            ON UPDATE RESTRICT;

-- Enforce one SOP per process.
-- MySQL allows many NULLs in a unique index, which is what we want.
CREATE UNIQUE INDEX uq_sop_business_process_id ON sop(process_id);