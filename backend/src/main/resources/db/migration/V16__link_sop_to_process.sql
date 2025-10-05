UPDATE sop s
    LEFT JOIN business_process bp
    ON bp.business_process_id = s.business_process_id
SET s.business_process_id = NULL
WHERE s.business_process_id IS NOT NULL
  AND bp.business_process_id IS NULL;

-- (Optional but good) 3) Add an index for the FK column to avoid table scans
CREATE INDEX idx_sop_business_process_id
    ON sop (business_process_id);