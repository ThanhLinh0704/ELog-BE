-- V61__fix_kh0200_null_vehicle_weight_limit.sql
-- Data bug: store KH0200 (id 176) is the only store in the whole dataset with
-- max_allowed_vehicle_weight set to 0.000 instead of NULL (every other store, including its
-- neighbor KH0199, is NULL = "no limit"). Since CapacityValidationServiceImpl only applies this
-- constraint when the column is non-NULL, a 0kg limit rejects every vehicle unconditionally
-- (no vehicle has a payload <= 0kg) — this alone was failing capacity validation for the whole
-- of RT-T01 regardless of fleet availability. Almost certainly a blank Excel cell that got
-- imported as 0 instead of NULL in V55. Reverting to NULL (no limit), matching every other store.

UPDATE stores
SET max_allowed_vehicle_weight = NULL
WHERE code = 'KH0200';
