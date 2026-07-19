-- US-08: Remove unique constraint uq_batch_active_date to support cumulative imports (multiple active batches per delivery date)
ALTER TABLE import_batches DROP INDEX uq_batch_active_date;
