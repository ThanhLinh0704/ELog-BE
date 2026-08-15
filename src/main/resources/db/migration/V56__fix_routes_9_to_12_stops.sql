-- ==========================================================================
-- V56: Fix Route Stops for Routes 9, 10, 11, 12
-- Resolves issue where Route 9, 10, 11 stops were deleted in V55 and failed to update,
-- leaving Route 9, 10, 11 empty and Route 12 containing legacy Route 9 stops.
-- ==========================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Ensure Route Names and Codes are correctly set for Routes 9 to 12
INSERT INTO routes (id, code, name, description, is_active) VALUES
(9, 'RT-T09', 'Tuyến 9: Lạng Sơn', 'Tuyến giao hàng khu vực Lạng Sơn', TRUE),
(10, 'RT-T10', 'Tuyến 10: Bắc Kạn', 'Tuyến giao hàng khu vực Bắc Kạn', TRUE),
(11, 'RT-T11', 'Tuyến 11: Hải Phòng', 'Tuyến giao hàng khu vực Hải Phòng', TRUE),
(12, 'RT-T12', 'Tuyến 12: Phía Nam - Tây Hà Nội', 'Tuyến giao hàng khu vực Chương Mỹ, Mỹ Đức, Thanh Oai, Phú Xuyên', TRUE)
ON DUPLICATE KEY UPDATE 
    code = VALUES(code),
    name = VALUES(name),
    description = VALUES(description),
    is_active = VALUES(is_active);

-- 2. Clean up route_stops for routes 9, 10, 11, 12 and target stores to prevent duplicate constraints
DELETE FROM route_stops WHERE route_id IN (9, 10, 11, 12);
DELETE FROM route_stops WHERE store_id IN (
    52, 49, 74, 47, 48, 51, 76,
    62, 59, 53, 81, 56, 82, 84,
    121, 119, 113, 123, 115, 114, 122, 118, 112, 120, 116,
    149, 148, 144, 143, 141, 146, 140, 24
);

-- 3. Insert correct route_stops entries for Route 9 (Lạng Sơn - 7 stops)
INSERT INTO route_stops (route_id, store_id, sequence_order) VALUES
(9, 52, 1), -- KH0052 - Điện Máy Thu Hiền LS
(9, 49, 2), -- KH0049 - Điện Máy Thanh Tâm LS
(9, 74, 3), -- KH0074 - Điện Máy Trâm Hằng TN
(9, 47, 4), -- KH0047 - Điện Máy Hoa Thọ LS
(9, 48, 5), -- KH0048 - Điện Máy Trung Kiên LS
(9, 51, 6), -- KH0051 - Điện Máy Sơn Thùy LS
(9, 76, 7); -- KH0076 - SC Tùng Thành

-- 4. Insert correct route_stops entries for Route 10 (Bắc Kạn - 7 stops)
INSERT INTO route_stops (route_id, store_id, sequence_order) VALUES
(10, 62, 1), -- KH0062 - Điện Máy Luyến Na BK
(10, 59, 2), -- KH0059 - Điện Máy Thủy Tuế BK
(10, 53, 3), -- KH0053 - Điện Máy Nam Thủy BK
(10, 81, 4), -- KH0083 - Điện Máy Tuyết Khải BK
(10, 56, 5), -- KH0056 - Điện Máy Minh Chiến BK
(10, 82, 6), -- KH0084 - Điện Máy Hoa Lợi BK
(10, 84, 7); -- KH0086 - Điện Máy Thanh Điệp BK ( Thành Điệp BC )

-- 5. Insert correct route_stops entries for Route 11 (Hải Phòng - 11 stops)
INSERT INTO route_stops (route_id, store_id, sequence_order) VALUES
(11, 121, 1), -- KH0123 - Điện Máy Tấn Tài HD
(11, 119, 2), -- KH0121 - Điện Máy Nhật Bản HD
(11, 113, 3), -- KH0115 - Điện Máy Sơn Hà HP
(11, 123, 4), -- KH0125 - Điện Máy Tấn Tài HP
(11, 115, 5), -- KH0117 - Điện máy Tuyên Loan
(11, 114, 6), -- KH0116 - Điện Máy Quảng Thi HP
(11, 122, 7), -- KH0124 - Điện Tử - Điện Lạnh Nhật Minh HP
(11, 118, 8), -- KH0120 - Điện Máy Hoàng Khoát HP
(11, 112, 9), -- KH0114 - Điện máy Trường Hiền HP
(11, 120, 10), -- KH0122 - Trung Tâm Điện Máy Hưng Thúy HD
(11, 116, 11); -- KH0118 - Điện Máy Thủy Sản GL

-- 6. Insert correct route_stops entries for Route 12 (Phía Nam - Tây Hà Nội - 8 stops)
INSERT INTO route_stops (route_id, store_id, sequence_order) VALUES
(12, 149, 1), -- KH0151 - Điện Máy Thanh Bình BT
(12, 148, 2), -- KH0150 - Điện Máy Đức Huỳnh BT
(12, 144, 3), -- KH0146 - Điện Máy Thành Đạt BT
(12, 143, 4), -- KH0145 - Điện Máy Kiều Trụ BT
(12, 141, 5), -- KH0143 - Điện Máy Nghĩa Huệ BT
(12, 146, 6), -- KH0148 - Điện Máy Huy Khánh BT 2
(12, 140, 7), -- KH0142 - Điện máy Tuấn Sơn CS2 BT
(12, 24, 8);  -- KH0024 - Điện Máy Trường Thủy SS

-- 7. Reset polyline and distance cache for recalculation
UPDATE routes SET route_polyline = NULL, total_distance_km = NULL, total_duration_min = NULL WHERE id IN (9, 10, 11, 12);

SET FOREIGN_KEY_CHECKS = 1;
