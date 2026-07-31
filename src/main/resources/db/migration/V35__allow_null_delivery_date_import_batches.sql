-- Allow NULL for delivery_date in import_batches table to support multi-date Excel imports
ALTER TABLE import_batches MODIFY COLUMN delivery_date DATE NULL;
