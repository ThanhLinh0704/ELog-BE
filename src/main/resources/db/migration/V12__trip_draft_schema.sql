-- V12__trip_draft_schema.sql
-- US-10 Route Consolidation — TripDraft & TripDraftStop tables

CREATE TABLE trip_drafts (
    id                 BIGINT        AUTO_INCREMENT PRIMARY KEY,
    route_id           BIGINT        NOT NULL,
    delivery_date      DATE          NOT NULL,
    total_volume_m3    DECIMAL(12,6) NOT NULL DEFAULT 0,
    total_weight_kg    DECIMAL(12,3) NOT NULL DEFAULT 0,
    active_stop_count  INT           NOT NULL DEFAULT 0,
    skipped_stop_count INT           NOT NULL DEFAULT 0,
    status             VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_td_route FOREIGN KEY (route_id) REFERENCES routes(id),
    CONSTRAINT uq_td_route_date UNIQUE (route_id, delivery_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE trip_draft_stops (
    id             BIGINT  AUTO_INCREMENT PRIMARY KEY,
    trip_draft_id  BIGINT  NOT NULL,
    route_stop_id  BIGINT  NOT NULL,
    store_id       BIGINT  NOT NULL,
    sequence_no    INT     NOT NULL,
    is_active      BOOLEAN NOT NULL,
    order_count    INT     NOT NULL DEFAULT 0,
    CONSTRAINT fk_tds_tripdraft FOREIGN KEY (trip_draft_id) REFERENCES trip_drafts(id),
    CONSTRAINT fk_tds_routestop FOREIGN KEY (route_stop_id) REFERENCES route_stops(id),
    CONSTRAINT fk_tds_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uq_tds_draft_stop UNIQUE (trip_draft_id, route_stop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE orders
    ADD COLUMN trip_draft_id BIGINT NULL,
    ADD CONSTRAINT fk_order_tripdraft FOREIGN KEY (trip_draft_id) REFERENCES trip_drafts(id);
