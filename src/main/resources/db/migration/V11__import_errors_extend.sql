-- V11__import_errors_extend.sql — US-09 Import Validation
ALTER TABLE import_errors
    ADD COLUMN error_code VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN' AFTER row_num,
    ADD COLUMN field_name VARCHAR(50) NULL AFTER error_code;

CREATE INDEX idx_import_errors_code ON import_errors (import_batch_id, error_code);
