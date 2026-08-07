-- V40__trip_outcome_history_schema.sql
-- Fix delivery failure reason persistence and add trip outcome history audit schema

ALTER TABLE delivery_order_results
    ADD COLUMN reason_code    VARCHAR(50)  NULL,
    ADD COLUMN exception_text VARCHAR(1000) NULL;

CREATE TABLE trip_outcome_events (
    id                BIGINT        AUTO_INCREMENT PRIMARY KEY,
    trip_execution_id BIGINT        NOT NULL,
    trip_id           BIGINT        NOT NULL,
    event_type        VARCHAR(50)   NOT NULL,
    actor_type        VARCHAR(30)   NOT NULL,
    actor_id          BIGINT        NULL,
    actor_username    VARCHAR(50)   NULL,
    actor_role        VARCHAR(50)   NULL,
    occurred_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status_before     VARCHAR(30)   NULL,
    status_after      VARCHAR(30)   NULL,
    order_id          BIGINT        NULL,
    order_ref         VARCHAR(50)   NULL,
    stop_id           BIGINT        NULL,
    store_code        VARCHAR(20)   NULL,
    delivery_result   VARCHAR(30)   NULL,
    reason_code       VARCHAR(50)   NULL,
    exception_text    VARCHAR(1000) NULL,
    validation_note   VARCHAR(1000) NULL,
    route_code        VARCHAR(20)   NULL,
    delivery_date     DATE          NULL,
    driver_username   VARCHAR(50)   NULL,
    CONSTRAINT fk_toe_trip_execution FOREIGN KEY (trip_execution_id) REFERENCES trip_executions(id),
    CONSTRAINT fk_toe_trip           FOREIGN KEY (trip_id)           REFERENCES trips(trip_id),
    CONSTRAINT fk_toe_actor          FOREIGN KEY (actor_id)          REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_toe_trip_execution ON trip_outcome_events (trip_execution_id);
CREATE INDEX idx_toe_trip           ON trip_outcome_events (trip_id);
CREATE INDEX idx_toe_event_type     ON trip_outcome_events (event_type);
CREATE INDEX idx_toe_occurred_at    ON trip_outcome_events (occurred_at);
CREATE INDEX idx_toe_order          ON trip_outcome_events (order_id);
CREATE INDEX idx_toe_driver         ON trip_outcome_events (driver_username);
CREATE INDEX idx_toe_route_date     ON trip_outcome_events (route_code, delivery_date);
