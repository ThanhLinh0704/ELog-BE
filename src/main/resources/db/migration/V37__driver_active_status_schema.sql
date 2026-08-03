-- V37__driver_active_status_schema.sql
-- BR-DRV-01..06: Driver Active/Inactive operational status management

ALTER TABLE users
    ADD COLUMN phone_number VARCHAR(20) NULL AFTER email,
    ADD COLUMN driver_status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' AFTER phone_number,
    ADD COLUMN driver_inactive_reason_code VARCHAR(30) NULL,
    ADD COLUMN driver_inactive_reason_note VARCHAR(500) NULL,
    ADD COLUMN driver_status_updated_at DATETIME NULL,
    ADD COLUMN driver_status_updated_by BIGINT NULL,
    ADD CONSTRAINT fk_users_driver_status_updated_by FOREIGN KEY (driver_status_updated_by) REFERENCES users(id);

CREATE TABLE driver_status_history (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
    driver_id         BIGINT       NOT NULL,
    status_before     VARCHAR(10)  NULL,
    status_after      VARCHAR(10)  NOT NULL,
    reason_code       VARCHAR(30)  NULL,
    reason_note       VARCHAR(500) NULL,
    changed_by        BIGINT       NOT NULL,
    changed_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dsh_driver  FOREIGN KEY (driver_id)  REFERENCES users(id),
    CONSTRAINT fk_dsh_changer FOREIGN KEY (changed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_dsh_driver ON driver_status_history (driver_id);

-- Permission seeding (ID 19: driver:read, ID 20: driver:write)
INSERT INTO permissions (id, name, description) VALUES
(19, 'driver:read',  'View driver operational status and history'),
(20, 'driver:write', 'Change driver operational status (Active/Inactive)');

-- ADMIN (role_id = 1) -> driver:read, driver:write
INSERT INTO role_permissions (role_id, permission_id) VALUES (1, 19), (1, 20);

-- DISPATCHER (role_id = 2) -> driver:read
INSERT INTO role_permissions (role_id, permission_id) VALUES (2, 19);

-- LOGISTICS_MANAGER (role_id = 5) -> driver:read
INSERT INTO role_permissions (role_id, permission_id) VALUES (5, 19);
