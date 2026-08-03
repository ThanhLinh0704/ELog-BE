-- V38__two_vehicle_safety_buffer_config.sql
-- Seed capacity safety buffer ratio into system_config for vehicle recommendation engine

INSERT INTO system_config (config_key, config_value, description) VALUES
    ('CAPACITY_SAFETY_BUFFER_RATIO', '0.90', 'Tỷ lệ an toàn tải trọng/thể tích khi đề xuất xe (0.90 = 90%)');
