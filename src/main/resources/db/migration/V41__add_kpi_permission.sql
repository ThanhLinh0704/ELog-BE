-- ============================================================
-- US-19: KPI Dashboard — add kpi:read permission
-- ============================================================

INSERT INTO permissions (name, description) VALUES
('kpi:read', 'View KPI dashboard and metrics');

-- Grant kpi:read to SYSTEM_ADMIN (role_id = 1)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions WHERE name = 'kpi:read';

-- Grant kpi:read to DISPATCHER (role_id = 2)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 2, id FROM permissions WHERE name = 'kpi:read';

-- Grant kpi:read to LOGISTICS_MANAGER (role_id = 5)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 5, id FROM permissions WHERE name = 'kpi:read';
