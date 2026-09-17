-- Additive foundation. Existing MVP content and workflow tables remain untouched.
CREATE TABLE sop_document (
    document_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id INT NOT NULL,
    business_process_id INT NOT NULL,
    document_kind VARCHAR(30) NOT NULL DEFAULT 'CLIENT_SOP',
    product_key VARCHAR(100) NULL,
    current_revision_id BIGINT NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_document_org FOREIGN KEY (org_id) REFERENCES org(org_id),
    CONSTRAINT fk_document_process FOREIGN KEY (business_process_id) REFERENCES business_process(business_process_id),
    CONSTRAINT uq_document_product_key UNIQUE (product_key),
    CONSTRAINT ck_document_kind CHECK (
        (document_kind = 'CLIENT_SOP' AND product_key IS NULL) OR
        (document_kind = 'PRODUCT_GUIDE' AND product_key IS NOT NULL AND CHAR_LENGTH(TRIM(product_key)) > 0)),
    CONSTRAINT ck_document_version CHECK (lock_version >= 0)
) ENGINE=InnoDB;

CREATE TABLE sop_work_item (
    work_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    original_author_id INT NOT NULL,
    base_revision_id BIGINT NULL,
    current_candidate_id BIGINT NULL,
    state VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_work_item_document UNIQUE (document_id, work_item_id),
    CONSTRAINT fk_work_item_document FOREIGN KEY (document_id) REFERENCES sop_document(document_id),
    CONSTRAINT fk_work_item_author FOREIGN KEY (original_author_id) REFERENCES users(id),
    CONSTRAINT ck_work_item_state CHECK (state IN ('DRAFT', 'IN_REVIEW', 'PUBLISHED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT ck_work_item_candidate CHECK (state NOT IN ('IN_REVIEW', 'PUBLISHED', 'REJECTED') OR current_candidate_id IS NOT NULL),
    CONSTRAINT ck_work_item_version CHECK (lock_version >= 0)
) ENGINE=InnoDB;

CREATE TABLE sop_working_copy (
    working_copy_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    work_item_id BIGINT NOT NULL,
    editor_id INT NOT NULL,
    source_candidate_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    description LONGTEXT NOT NULL,
    details LONGTEXT NOT NULL,
    content_schema_version INT NOT NULL DEFAULT 1,
    state VARCHAR(30) NOT NULL DEFAULT 'EDITABLE',
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_copy_request UNIQUE (document_id, work_item_id, working_copy_id),
    CONSTRAINT fk_copy_request FOREIGN KEY (document_id, work_item_id) REFERENCES sop_work_item(document_id, work_item_id),
    CONSTRAINT fk_copy_editor FOREIGN KEY (editor_id) REFERENCES users(id),
    CONSTRAINT ck_copy_state CHECK (state IN ('EDITABLE', 'SUBMITTED', 'CANCELLED')),
    CONSTRAINT ck_copy_version CHECK (lock_version >= 0 AND content_schema_version > 0)
) ENGINE=InnoDB;

CREATE TABLE sop_revision (
    revision_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    work_item_id BIGINT NULL,
    source_working_copy_id BIGINT NULL,
    predecessor_candidate_id BIGINT NULL,
    submitted_by_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description LONGTEXT NOT NULL,
    details LONGTEXT NOT NULL,
    content_schema_version INT NOT NULL,
    content_checksum CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    provenance VARCHAR(30) NOT NULL,
    source_label VARCHAR(255) NULL,
    recorded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    effective_at DATETIME(6) NULL,
    CONSTRAINT uq_revision_document UNIQUE (document_id, revision_id),
    CONSTRAINT uq_revision_request UNIQUE (document_id, work_item_id, revision_id),
    CONSTRAINT uq_revision_source_copy UNIQUE (source_working_copy_id),
    CONSTRAINT fk_revision_document FOREIGN KEY (document_id) REFERENCES sop_document(document_id),
    CONSTRAINT fk_revision_request FOREIGN KEY (document_id, work_item_id) REFERENCES sop_work_item(document_id, work_item_id),
    CONSTRAINT fk_revision_copy FOREIGN KEY (document_id, work_item_id, source_working_copy_id)
        REFERENCES sop_working_copy(document_id, work_item_id, working_copy_id),
    CONSTRAINT fk_revision_predecessor FOREIGN KEY (document_id, work_item_id, predecessor_candidate_id)
        REFERENCES sop_revision(document_id, work_item_id, revision_id),
    CONSTRAINT fk_revision_submitter FOREIGN KEY (submitted_by_id) REFERENCES users(id),
    CONSTRAINT ck_revision_source CHECK (
        (provenance = 'AUTHORED' AND work_item_id IS NOT NULL AND source_working_copy_id IS NOT NULL) OR
        (provenance IN ('PRODUCT_RELEASE', 'IMPORTED', 'LEGACY') AND work_item_id IS NULL
            AND source_working_copy_id IS NULL AND predecessor_candidate_id IS NULL)),
    CONSTRAINT ck_revision_schema CHECK (content_schema_version > 0)
) ENGINE=InnoDB;

ALTER TABLE sop_document ADD CONSTRAINT fk_document_current
    FOREIGN KEY (document_id, current_revision_id) REFERENCES sop_revision(document_id, revision_id);
ALTER TABLE sop_work_item ADD CONSTRAINT fk_work_item_base
    FOREIGN KEY (document_id, base_revision_id) REFERENCES sop_revision(document_id, revision_id);
ALTER TABLE sop_work_item ADD CONSTRAINT fk_work_item_candidate
    FOREIGN KEY (document_id, work_item_id, current_candidate_id)
        REFERENCES sop_revision(document_id, work_item_id, revision_id);
ALTER TABLE sop_working_copy ADD CONSTRAINT fk_copy_candidate
    FOREIGN KEY (document_id, work_item_id, source_candidate_id)
        REFERENCES sop_revision(document_id, work_item_id, revision_id);

CREATE TABLE revision_participant (
    revision_id BIGINT NOT NULL,
    user_id INT NOT NULL,
    contribution_type VARCHAR(30) NOT NULL,
    PRIMARY KEY (revision_id, user_id, contribution_type),
    CONSTRAINT fk_participant_revision FOREIGN KEY (revision_id) REFERENCES sop_revision(revision_id),
    CONSTRAINT fk_participant_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT ck_participant_type CHECK (contribution_type IN ('AUTHOR', 'EDITOR', 'SUBMITTER'))
) ENGINE=InnoDB;

CREATE TABLE revision_history_position (
    document_id BIGINT NOT NULL,
    revision_id BIGINT NOT NULL,
    ordering_key DECIMAL(65,0) NOT NULL,
    PRIMARY KEY (document_id, revision_id),
    CONSTRAINT uq_history_position UNIQUE (document_id, ordering_key),
    CONSTRAINT fk_history_revision FOREIGN KEY (document_id, revision_id) REFERENCES sop_revision(document_id, revision_id)
) ENGINE=InnoDB;
