-- V15__capacity_validation_fields.sql
-- US-12 Capacity Validation

ALTER TABLE trip_drafts
    ADD COLUMN volume_check_result ENUM('NOT_CHECKED','PASS','FAIL') NOT NULL DEFAULT 'NOT_CHECKED',
    ADD COLUMN weight_check_result ENUM('NOT_CHECKED','PASS','FAIL') NOT NULL DEFAULT 'NOT_CHECKED',
    ADD COLUMN validated_at   DATETIME NULL,
    ADD COLUMN validated_by   BIGINT   NULL,
    ADD CONSTRAINT fk_td_validated_by
        FOREIGN KEY (validated_by) REFERENCES users(id);
