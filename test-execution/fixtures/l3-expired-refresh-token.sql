-- Test-only fixture for L3-AUTH-122. Run only against a dedicated elog_l3_qa_* schema.
DELETE FROM refresh_tokens WHERE token = 'L3QA_EXPIRED_REFRESH_TOKEN';
INSERT INTO refresh_tokens (token, user_id, expiry_date, created_at)
SELECT 'L3QA_EXPIRED_REFRESH_TOKEN', id, UTC_TIMESTAMP() - INTERVAL 1 DAY, UTC_TIMESTAMP() - INTERVAL 2 DAY
FROM users
WHERE username = 'dispatcher01';
