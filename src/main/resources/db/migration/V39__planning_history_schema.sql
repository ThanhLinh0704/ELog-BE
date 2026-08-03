-- V39__planning_history_schema.sql
-- Audit trail schema for Trip Draft & Trip planning lifecycle events

CREATE TABLE trip_planning_events (
    id                BIGINT        AUTO_INCREMENT PRIMARY KEY,
    trip_draft_id     BIGINT        NULL,
    trip_id           BIGINT        NULL,
    event_type        VARCHAR(50)   NOT NULL,
    actor_type        VARCHAR(30)   NOT NULL,
    actor_id          BIGINT        NULL,
    actor_username    VARCHAR(50)   NULL,
    actor_role        VARCHAR(50)   NULL,
    occurred_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status_before     VARCHAR(20)   NULL,
    status_after      VARCHAR(20)   NULL,
    change_summary    VARCHAR(500)  NULL,
    change_detail     JSON          NULL,
    note              VARCHAR(1000) NULL,
    plan_version      INT           NULL,
    option_code       VARCHAR(100)  NULL,
    route_code        VARCHAR(20)   NULL,
    delivery_date     DATE          NULL,
    CONSTRAINT fk_tpe_trip_draft FOREIGN KEY (trip_draft_id) REFERENCES trip_drafts(id),
    CONSTRAINT fk_tpe_trip       FOREIGN KEY (trip_id)       REFERENCES trips(trip_id),
    CONSTRAINT fk_tpe_actor      FOREIGN KEY (actor_id)      REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_tpe_trip_draft   ON trip_planning_events (trip_draft_id);
CREATE INDEX idx_tpe_trip         ON trip_planning_events (trip_id);
CREATE INDEX idx_tpe_event_type   ON trip_planning_events (event_type);
CREATE INDEX idx_tpe_occurred_at  ON trip_planning_events (occurred_at);
CREATE INDEX idx_tpe_actor        ON trip_planning_events (actor_id);
CREATE INDEX idx_tpe_route_date   ON trip_planning_events (route_code, delivery_date);

-- Permission seeding (ID 21: planning-history:read)
INSERT INTO permissions (id, name, description) VALUES
(21, 'planning-history:read', 'View trip draft / trip planning history (audit trail)');

INSERT INTO role_permissions (role_id, permission_id) VALUES (1, 21);
INSERT INTO role_permissions (role_id, permission_id) VALUES (2, 21);
INSERT INTO role_permissions (role_id, permission_id) VALUES (5, 21);
