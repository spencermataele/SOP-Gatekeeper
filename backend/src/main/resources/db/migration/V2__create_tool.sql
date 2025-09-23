CREATE TABLE IF NOT EXISTS tool (
    tool_id INT PRIMARY KEY AUTO_INCREMENT,
    tool_name VARCHAR(255) NOT NULL,
    sop_id INT NOT NULL,
    INDEX idx_tool_sop_id (sop_id),
    CONSTRAINT fk_tool_sop
        FOREIGN KEY (sop_id) REFERENCES sop(sop_id)
            ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;