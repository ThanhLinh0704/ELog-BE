-- ============================================================
-- ELog Delivery Management System
-- Reference / seed data for permissions system
-- ============================================================

-- 1. Create permissions table
CREATE TABLE permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Create role_permissions join table
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Seed permissions
INSERT INTO permissions (id, name, description) VALUES
(1, 'user:read', 'View user accounts'),
(2, 'user:write', 'Create, update, or disable user accounts'),
(3, 'role:read', 'View user roles'),
(4, 'role:write', 'Assign or update roles for users'),
(5, 'vehicle:read', 'View vehicle specifications and availability'),
(6, 'vehicle:write', 'Create, update, or disable vehicles'),
(7, 'route:read', 'View fixed delivery routes'),
(8, 'route:write', 'Create or update fixed delivery routes'),
(9, 'store:read', 'View store branches and coordinate metadata'),
(10, 'store:write', 'Create or update store branches'),
(11, 'order:import', 'Upload and import Excel order sheet'),
(12, 'trip:read', 'View trip lists, status, and manifests'),
(13, 'trip:write', 'Create or update trip drafts'),
(14, 'trip:confirm', 'Confirm loading capacity or route plans'),
(15, 'trip:coordinate', 'Coordinate dispatching, vehicle assignment, and locking trips'),
(16, 'trip:execute', 'Driver trip execution, check-ins, e-POD submissions');

-- 4. Seed role_permissions based on roles
-- SYSTEM_ADMIN (role_id = 1) -> gets all permissions (1 to 16)
INSERT INTO role_permissions (role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 15), (1, 16);

-- DISPATCHER (role_id = 2) -> route:read, store:read, vehicle:read, order:import, trip:read, trip:write, trip:confirm, trip:coordinate
INSERT INTO role_permissions (role_id, permission_id) VALUES
(2, 5), (2, 7), (2, 9), (2, 11), (2, 12), (2, 13), (2, 14), (2, 15);

-- WAREHOUSE_STAFF (role_id = 3) -> trip:read, trip:write (manifest updates, load confirmation)
INSERT INTO role_permissions (role_id, permission_id) VALUES
(3, 12), (3, 13);

-- DRIVER (role_id = 4) -> trip:read, trip:execute
INSERT INTO role_permissions (role_id, permission_id) VALUES
(4, 12), (4, 16);

-- LOGISTICS_MANAGER (role_id = 5) -> trip:read, route:read, store:read, vehicle:read
INSERT INTO role_permissions (role_id, permission_id) VALUES
(5, 5), (5, 7), (5, 9), (5, 12);
