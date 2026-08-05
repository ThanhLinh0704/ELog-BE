-- V43__seed_missing_system_user.sql
-- ============================================================
-- Khôi phục user id=1 ("system") — bị thiếu từ V2__seed_reference_data.sql
-- (mục "Default Admin User" chỉ có comment, thiếu câu INSERT).
-- 3 nơi trong code (TripMonitoringServiceImpl, TimeExceptionDetectionJob,
-- ExceptionServiceImpl) đều hard-code SYSTEM_USER_ID = 1L để ghi reported_by
-- cho các bản ghi DeliveryException do hệ thống tự tạo (BR-09 TIME_EXCEPTION).
-- ============================================================

INSERT INTO users (id, username, email, password_hash, full_name, is_active) VALUES
(1, 'system', 'system@elog.internal',
 '$2a$12$dfT1VXhfjzIm5GRAjwgYg.5O8XQ.mCMmneaFuXSOLrpXGRRNApJxG',
 'System', TRUE);

INSERT INTO user_roles (user_id, role_id)
SELECT 1, id FROM roles WHERE name = 'SYSTEM_ADMIN';
