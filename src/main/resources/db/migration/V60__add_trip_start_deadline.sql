-- V60__add_trip_start_deadline.sql
-- "Chặn bắt đầu chuyến quá hạn": driver must tap "Bắt đầu chuyến" within N minutes of vehicle
-- assignment (trips.locked_at), or the start action is rejected server-side and the dispatcher
-- gets an immediate exception flag. See filemd/new-bug.md.

-- 1. Widen the exception_type ENUM to accept the new auto-flag type.
--    NOTE: this also retroactively fixes TRIP_STALE_UNSTARTED (added to the Java enum in V59's
--    session but never added here) — until now, TripStaleDetectionJob's save() would have failed
--    or been truncated against this column, since the ENUM still only listed the original 2 values.
ALTER TABLE delivery_exceptions
    MODIFY COLUMN exception_type
        ENUM('TIME_EXCEPTION','DELIVERY_REJECTION','TRIP_STALE_UNSTARTED','TRIP_START_DEADLINE_EXCEEDED')
        NOT NULL;

-- 2. Deadline (minutes) between vehicle assignment (locked_at) and the driver tapping "Bắt đầu
--    chuyến". Configurable without a code change.
INSERT INTO system_config (config_key, config_value, description) VALUES
    ('TRIP_START_DEADLINE_MINUTES', '15',
     'So phut toi da tinh tu luc dieu phoi gan xe (trips.locked_at) ma tai xe phai bam Bat dau chuyen, qua han thi bi khoa');
