-- ==========================================================================
-- MASTER BACKUP DATA FOR ALL RENUMBERED ACTIVE ROUTES (1 TO 12)
-- INCLUDES DELETIONS FOR HISTORICALLY REMOVED ROUTES AND STORE KH0080
-- Generated from current local database (elog_db)
-- ==========================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- --------------------------------------------------------------------------
-- 0. DELETIONS FOR HISTORICALLY REMOVED STORES (INCLUDING KH0080) AND OLD ROUTE IDS
-- --------------------------------------------------------------------------
DELETE FROM orders WHERE store_id IN (1, 4, 6, 7, 8, 10, 11, 12, 13, 26, 27, 28, 29, 30, 31, 32, 34, 36, 37, 38, 39, 54, 55, 57, 58, 60, 61, 65, 67, 68, 79, 83, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106);
DELETE FROM trip_draft_stops WHERE store_id IN (1, 4, 6, 7, 8, 10, 11, 12, 13, 26, 27, 28, 29, 30, 31, 32, 34, 36, 37, 38, 39, 54, 55, 57, 58, 60, 61, 65, 67, 68, 79, 83, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106);
DELETE FROM route_stops WHERE store_id IN (1, 4, 6, 7, 8, 10, 11, 12, 13, 26, 27, 28, 29, 30, 31, 32, 34, 36, 37, 38, 39, 54, 55, 57, 58, 60, 61, 65, 67, 68, 79, 83, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106);
DELETE FROM stores WHERE id IN (1, 4, 6, 7, 8, 10, 11, 12, 13, 26, 27, 28, 29, 30, 31, 32, 34, 36, 37, 38, 39, 54, 55, 57, 58, 60, 61, 65, 67, 68, 79, 83, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106) OR code = 'KH0080';
DELETE FROM trip_drafts WHERE route_id IN (9, 10, 11, 13, 16, 14, 15, 17);
DELETE FROM route_stops WHERE route_id IN (9, 10, 11, 13, 16, 14, 15, 17);
DELETE FROM routes WHERE id IN (13, 14, 15, 16, 17);

-- --------------------------------------------------------------------------
-- 1. ROUTES INFORMATION (12 active routes, renumbered 1 to 12)
-- --------------------------------------------------------------------------
-- Route 1: RT-T01 - Tuyến 1: Nội thành & Phía Bắc - Tây HN
INSERT INTO routes (id, code, name, description, is_active)
VALUES (1, 'RT-T01', 'Tuyến 1: Nội thành & Phía Bắc - Tây HN', 'Tuyến giao hàng khu vực Sơn Tây, Ba Vì, Thạch Thất', TRUE)
AS r ON DUPLICATE KEY UPDATE code = r.code, name = r.name, description = r.description, is_active = r.is_active;

-- Route 2: RT-T02 - Tuyến 2: Hà Nam — Nam Định
INSERT INTO routes (id, code, name, description, is_active)
VALUES (2, 'RT-T02', 'Tuyến 2: Hà Nam — Nam Định', 'Tuyến giao hàng khu vực Hà Nam và Nam Định', TRUE)
AS r ON DUPLICATE KEY UPDATE code = r.code, name = r.name, description = r.description, is_active = r.is_active;

-- Route 3: RT-T03 - Tuyến 3: Ninh Bình — Hòa Bình
INSERT INTO routes (id, code, name, description, is_active)
VALUES (3, 'RT-T03', 'Tuyến 3: Ninh Bình — Hòa Bình', 'Tuyến giao hàng khu vực Ninh Bình và Hòa Bình', TRUE)
AS r ON DUPLICATE KEY UPDATE code = r.code, name = r.name, description = r.description, is_active = r.is_active;

-- Route 4: RT-T04 - Tuyến 4: Hưng Yên
INSERT INTO routes (id, code, name, description, is_active)
VALUES (4, 'RT-T04', 'Tuyến 4: Hưng Yên', 'Tuyến giao hàng khu vực Hưng Yên', TRUE)
AS r ON DUPLICATE KEY UPDATE code = r.code, name = r.name, description = r.description, is_active = r.is_active;

-- Route 5: RT-T05 - Tuyến 5: Thái Bình
INSERT INTO routes (id, code, name, description, is_active)
VALUES (5, 'RT-T05', 'Tuyến 5: Thái Bình', 'Tuyến giao hàng khu vực Thái Bình', TRUE)
AS r ON DUPLICATE KEY UPDATE code = r.code, name = r.name, description = r.description, is_active = r.is_active;

-- Route 6: RT-T06 - Tuyến 6: Bắc Ninh — Bắc Giang — Quảng Ninh
INSERT INTO routes (id, code, name, description, is_active)
VALUES (6, 'RT-T06', 'Tuyến 6: Bắc Ninh — Bắc Giang — Quảng Ninh', 'Tuyến giao hàng khu vực Bắc Ninh, Bắc Giang, Quảng Ninh', TRUE)
AS r ON DUPLICATE KEY UPDATE code = r.code, name = r.name, description = r.description, is_active = r.is_active;

-- Route 7: RT-T07 - Tuyến 7: Vĩnh Phúc — Phú Thọ
INSERT INTO routes (id, code, name, description, is_active)
VALUES (7, 'RT-T07', 'Tuyến 7: Vĩnh Phúc — Phú Thọ', 'Tuyến giao hàng khu vực Vĩnh Phúc và Phú Thọ', TRUE)
AS r ON DUPLICATE KEY UPDATE code = r.code, name = r.name, description = r.description, is_active = r.is_active;

-- Route 8: RT-T08 - Tuyến 8: Thái Nguyên — Tuyên Quang
INSERT INTO routes (id, code, name, description, is_active)
VALUES (8, 'RT-T08', 'Tuyến 8: Thái Nguyên — Tuyên Quang', 'Tuyến giao hàng khu vực Thái Nguyên và Tuyên Quang', TRUE)
AS r ON DUPLICATE KEY UPDATE code = r.code, name = r.name, description = r.description, is_active = r.is_active;

-- Route 9: RT-T09 - Tuyến 9: Lạng Sơn
INSERT INTO routes (id, code, name, description, is_active)
VALUES (9, 'RT-T09', 'Tuyến 9: Lạng Sơn', 'Tuyến giao hàng khu vực Lạng Sơn', TRUE)
AS r ON DUPLICATE KEY UPDATE code = r.code, name = r.name, description = r.description, is_active = r.is_active;

-- Route 10: RT-T10 - Tuyến 10: Bắc Kạn
INSERT INTO routes (id, code, name, description, is_active)
VALUES (10, 'RT-T10', 'Tuyến 10: Bắc Kạn', 'Tuyến giao hàng khu vực Bắc Kạn', TRUE)
AS r ON DUPLICATE KEY UPDATE code = r.code, name = r.name, description = r.description, is_active = r.is_active;

-- Route 11: RT-T11 - Tuyến 11: Hải Phòng
INSERT INTO routes (id, code, name, description, is_active)
VALUES (11, 'RT-T11', 'Tuyến 11: Hải Phòng', 'Tuyến giao hàng khu vực Hải Phòng', TRUE)
AS r ON DUPLICATE KEY UPDATE code = r.code, name = r.name, description = r.description, is_active = r.is_active;

-- Route 12: RT-T12 - Tuyến 12: Phía Nam - Tây Hà Nội
INSERT INTO routes (id, code, name, description, is_active)
VALUES (12, 'RT-T12', 'Tuyến 12: Phía Nam - Tây Hà Nội', 'Tuyến giao hàng khu vực Chương Mỹ, Mỹ Đức, Thanh Oai, Phú Xuyên', TRUE)
AS r ON DUPLICATE KEY UPDATE code = r.code, name = r.name, description = r.description, is_active = r.is_active;

-- --------------------------------------------------------------------------
-- 2. STORES INFORMATION (Total 119 stores attached to routes 1-12)
-- --------------------------------------------------------------------------
-- Store ID 2: KH0002 - HỘ KINH DOANH PHẠM NHƯ DŨNG
UPDATE stores SET
    name = 'HỘ KINH DOANH PHẠM NHƯ DŨNG',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '17',
    district_code = '159',
    ward_code = '05395',
    address_detail = 'Số 43, Đường Quang Trung',
    latitude = '20.4850000',
    longitude = '105.7350000',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 2 AND code = 'KH0002';

-- Store ID 3: KH0003 - Điện Máy Lâm Oanh BT
UPDATE stores SET
    name = 'Điện Máy Lâm Oanh BT',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '17',
    district_code = '155',
    ward_code = '05128',
    address_detail = 'Số 308, Xóm Đầm',
    latitude = '20.6120000',
    longitude = '105.2850000',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 3 AND code = 'KH0003';

-- Store ID 5: KH0005 - Điện máy Tất Thành
UPDATE stores SET
    name = 'Điện máy Tất Thành',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '17',
    district_code = '155',
    ward_code = '05140',
    address_detail = 'Số 41, Phú Cường',
    latitude = '20.6505690',
    longitude = '105.1562340',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 5 AND code = 'KH0005';

-- Store ID 9: KH0009 - Điện Máy Huyền Linh TQ
UPDATE stores SET
    name = 'Điện Máy Huyền Linh TQ',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '08',
    district_code = '070',
    ward_code = '02200',
    address_detail = 'Số 128, Đường Quang Trung',
    latitude = '21.8201950',
    longitude = '105.2038710',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 9 AND code = 'KH0009';

-- Store ID 14: KH0014 - Điện Máy Trường Giang PT 1
UPDATE stores SET
    name = 'Điện Máy Trường Giang PT 1',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '25',
    district_code = '238',
    ward_code = '08542',
    address_detail = 'Số 47, Phố 19/5',
    latitude = '21.2052640',
    longitude = '105.1772210',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 14 AND code = 'KH0014';

-- Store ID 15: KH0015 - Điện Máy Hiền Hằng PT
UPDATE stores SET
    name = 'Điện Máy Hiền Hằng PT',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '25',
    district_code = '232',
    ward_code = '08152',
    address_detail = 'Số 187, Đường Phạm Tiến Duật',
    latitude = '21.5016790',
    longitude = '105.1436650',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 15 AND code = 'KH0015';

-- Store ID 16: KH0016 - Điện Máy Anh Tuấn PT
UPDATE stores SET
    name = 'Điện Máy Anh Tuấn PT',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '25',
    district_code = '228',
    ward_code = '07966',
    address_detail = 'Số 122, Khu 6',
    latitude = '21.4152650',
    longitude = '105.1946430',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 16 AND code = 'KH0016';

-- Store ID 17: KH0017 - Điện Máy Thanh Vinh PT
UPDATE stores SET
    name = 'Điện Máy Thanh Vinh PT',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '25',
    district_code = '239',
    ward_code = '08674',
    address_detail = 'Số 157, Đường Đôi Đảo Ngọc Xanh, Đông Lâm',
    latitude = '21.1664100',
    longitude = '105.2800710',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 17 AND code = 'KH0017';

-- Store ID 18: KH0018 - Cửa Hàng Lan Thành PT
UPDATE stores SET
    name = 'Cửa Hàng Lan Thành PT',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '25',
    district_code = '228',
    ward_code = '07951',
    address_detail = 'Số 23, Khu 7',
    latitude = '21.4183900',
    longitude = '105.2429710',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 18 AND code = 'KH0018';

-- Store ID 19: KH0019 - Điện Máy Toàn Lan VP
UPDATE stores SET
    name = 'Điện Máy Toàn Lan VP',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '26',
    district_code = '247',
    ward_code = '08881',
    address_detail = 'Số 158, Làng Quế',
    latitude = '21.3638850',
    longitude = '105.5796370',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 19 AND code = 'KH0019';

-- Store ID 20: KH0020 - Điện Máy Quỳnh Vân VP
UPDATE stores SET
    name = 'Điện Máy Quỳnh Vân VP',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '26',
    district_code = '246',
    ward_code = '08761',
    address_detail = 'Số 119, Thôn Phú Thượng',
    latitude = '21.4120670',
    longitude = '105.4637650',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 20 AND code = 'KH0020';

-- Store ID 21: KH0021 - Hoàng Thị Việt Hà -Điện Máy Quang Hà PT
UPDATE stores SET
    name = 'Hoàng Thị Việt Hà -Điện Máy Quang Hà PT',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '25',
    district_code = '233',
    ward_code = '08230',
    address_detail = 'Số 149, Khu Rừng Mận',
    latitude = '21.4157320',
    longitude = '105.3001400',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 21 AND code = 'KH0021';

-- Store ID 22: KH0022 - Điện Tử - Điện Lạnh - Điện Dân Dụng - Thiết Bị Âm Thanh Thanh Hạnh VP
UPDATE stores SET
    name = 'Điện Tử - Điện Lạnh - Điện Dân Dụng - Thiết Bị Âm Thanh Thanh Hạnh VP',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '26',
    district_code = '251',
    ward_code = '09025',
    address_detail = 'Số 135, Đường Phùng Bá Kỳ',
    latitude = '21.2376360',
    longitude = '105.5807740',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 22 AND code = 'KH0022';

-- Store ID 23: KH0023 - Kim Khí Tổng Hợp  Phương Nam VP
UPDATE stores SET
    name = 'Kim Khí Tổng Hợp  Phương Nam VP',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '26',
    district_code = '252',
    ward_code = '09076',
    address_detail = 'Số 168, Đường Phan Bội Châu',
    latitude = '21.2180750',
    longitude = '105.5133380',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 23 AND code = 'KH0023';

-- Store ID 24: KH0024 - Điện Máy Trường Thủy SS
UPDATE stores SET
    name = 'Điện Máy Trường Thủy SS',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '01',
    district_code = '280',
    ward_code = '10273',
    address_detail = 'Số 125, Đường Trần Phú',
    latitude = '20.7447130',
    longitude = '105.9116780',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 24 AND code = 'KH0024';

-- Store ID 25: KH0025 - Điện Máy Ngọc Phong
UPDATE stores SET
    name = 'Điện Máy Ngọc Phong',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '17',
    district_code = '156',
    ward_code = '05200',
    address_detail = 'Số 95, Mai Châu',
    latitude = '20.6652260',
    longitude = '105.0835300',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 25 AND code = 'KH0025';

-- Store ID 40: KH0040 - Điện Máy  Điện Giang BG
UPDATE stores SET
    name = 'Điện Máy  Điện Giang BG',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '24',
    district_code = '223',
    ward_code = '07840',
    address_detail = 'Số 68, TDP Số 1',
    latitude = '21.3549620',
    longitude = '105.9781540',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 40 AND code = 'KH0040';

-- Store ID 41: KH0041 - Điện Máy Luyện Thanh BG
UPDATE stores SET
    name = 'Điện Máy Luyện Thanh BG',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '24',
    district_code = '216',
    ward_code = '07339',
    address_detail = 'Số 291, Đường Nguyễn Trãi',
    latitude = '21.3856650',
    longitude = '106.1236790',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 41 AND code = 'KH0041';

-- Store ID 42: KH0042 - Điện Máy Thá Phượng BN
UPDATE stores SET
    name = 'Điện Máy Thá Phượng BN',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '27',
    district_code = '264',
    ward_code = '09523',
    address_detail = 'Số 282, Đường Trừng Xá',
    latitude = '21.0263920',
    longitude = '106.2445190',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 42 AND code = 'KH0042';

-- Store ID 43: KH0043 - Điện Máy Tuấn Hưng BG
UPDATE stores SET
    name = 'Điện Máy Tuấn Hưng BG',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '24',
    district_code = '219',
    ward_code = '07582',
    address_detail = 'Số 10, Phố Mai Tô',
    latitude = '21.3861530',
    longitude = '106.6717620',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 43 AND code = 'KH0043';

-- Store ID 44: KH0044 - Điện Máy Duy Quang BG
UPDATE stores SET
    name = 'Điện Máy Duy Quang BG',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '24',
    district_code = '217',
    ward_code = '07375',
    address_detail = 'Số 58, Đường Nguyễn Xuân Lan',
    latitude = '21.3521190',
    longitude = '106.2598070',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 44 AND code = 'KH0044';

-- Store ID 45: KH0045 - Điện Máy Tân Tiến BN
UPDATE stores SET
    name = 'Điện Máy Tân Tiến BN',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '27',
    district_code = '262',
    ward_code = '09400',
    address_detail = 'Số 147, Đường Lạc Long Quân',
    latitude = '21.0352590',
    longitude = '106.0775460',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 45 AND code = 'KH0045';

-- Store ID 46: KH0046 - Điện Máy Chiến Lan BN
UPDATE stores SET
    name = 'Điện Máy Chiến Lan BN',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '27',
    district_code = '264',
    ward_code = '09496',
    address_detail = 'Số 55, Đường Hoàng Văn Thụ',
    latitude = '21.0257000',
    longitude = '106.2409940',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 46 AND code = 'KH0046';

-- Store ID 47: KH0047 - Điện Máy Hoa Thọ LS
UPDATE stores SET
    name = 'Điện Máy Hoa Thọ LS',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '20',
    district_code = '182',
    ward_code = '06124',
    address_detail = 'Số 171, Đường Nguyễn Trãi',
    latitude = '22.0315197',
    longitude = '106.5713194',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 47 AND code = 'KH0047';

-- Store ID 48: KH0048 - Điện Máy Trung Kiên LS
UPDATE stores SET
    name = 'Điện Máy Trung Kiên LS',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '20',
    district_code = '178',
    ward_code = '05971',
    address_detail = 'Số 24, Đường Ngô Quyền',
    latitude = '21.8535079',
    longitude = '106.7684579',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 48 AND code = 'KH0048';

-- Store ID 49: KH0049 - Điện Máy Thanh Tâm LS
UPDATE stores SET
    name = 'Điện Máy Thanh Tâm LS',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '20',
    district_code = '181',
    ward_code = '06112',
    address_detail = 'Số 307, Đường Trần Phú',
    latitude = '22.0597300',
    longitude = '106.2721417',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 49 AND code = 'KH0049';

-- Store ID 50: KH0050 - Trung Tâm Điện Máy Tuấn Trang BG
UPDATE stores SET
    name = 'Trung Tâm Điện Máy Tuấn Trang BG',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '27',
    district_code = '263',
    ward_code = '09454',
    address_detail = 'Số 184, Đường Nguyễn Văn Cừ',
    latitude = '21.0526680',
    longitude = '106.1706880',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 50 AND code = 'KH0050';

-- Store ID 51: KH0051 - Điện Máy Sơn Thùy LS
UPDATE stores SET
    name = 'Điện Máy Sơn Thùy LS',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '20',
    district_code = '188',
    ward_code = '06592',
    address_detail = 'Số 160, Đường Ngô Quyền',
    latitude = '21.6694292',
    longitude = '106.9123631',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 51 AND code = 'KH0051';

-- Store ID 52: KH0052 - Điện Máy Thu Hiền LS
UPDATE stores SET
    name = 'Điện Máy Thu Hiền LS',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '20',
    district_code = '185',
    ward_code = '06325',
    address_detail = 'Số 204, Đường Lý Thường Kiệt',
    latitude = '21.8144080',
    longitude = '106.2513753',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 52 AND code = 'KH0052';

-- Store ID 53: KH0053 - Điện Máy Nam Thủy BK
UPDATE stores SET
    name = 'Điện Máy Nam Thủy BK',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '06',
    district_code = '064',
    ward_code = '02020',
    address_detail = 'Số 156, Đường Phan Đình Phùng',
    latitude = '22.2728552',
    longitude = '105.8564601',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 53 AND code = 'KH0053';

-- Store ID 56: KH0056 - Điện Máy Minh Chiến BK
UPDATE stores SET
    name = 'Điện Máy Minh Chiến BK',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '06',
    district_code = '061',
    ward_code = '01888',
    address_detail = 'Số 35, Đường Nguyễn Trãi',
    latitude = '22.2769390',
    longitude = '105.8716000',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 56 AND code = 'KH0056';

-- Store ID 59: KH0059 - Điện Máy Thủy Tuế BK
UPDATE stores SET
    name = 'Điện Máy Thủy Tuế BK',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '06',
    district_code = '066',
    ward_code = '02155',
    address_detail = 'Số 258, Đường Nguyễn Huệ',
    latitude = '22.2584190',
    longitude = '105.8591390',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 59 AND code = 'KH0059';

-- Store ID 62: KH0062 - Điện Máy Luyến Na BK
UPDATE stores SET
    name = 'Điện Máy Luyến Na BK',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '06',
    district_code = '065',
    ward_code = '02086',
    address_detail = 'Số 268, Đường Trường Chinh',
    latitude = '22.2523920',
    longitude = '105.8343500',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 62 AND code = 'KH0062';

-- Store ID 63: KH0063 - Điện Máy Thành Tuyên TQ
UPDATE stores SET
    name = 'Điện Máy Thành Tuyên TQ',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '08',
    district_code = '074',
    ward_code = '02374',
    address_detail = 'Số 190, Đường dân cư xóm 5',
    latitude = '22.0381170',
    longitude = '105.0419500',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 63 AND code = 'KH0063';

-- Store ID 64: KH0064 - Điện máy Anh Quảng TQ
UPDATE stores SET
    name = 'Điện máy Anh Quảng TQ',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '08',
    district_code = '076',
    ward_code = '02536',
    address_detail = 'Số 312, Đường Thanh Niên',
    latitude = '21.6989990',
    longitude = '105.3907770',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 64 AND code = 'KH0064';

-- Store ID 66: KH0066 - Điện Máy Minh Tuyên TQ
UPDATE stores SET
    name = 'Điện Máy Minh Tuyên TQ',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '08',
    district_code = '073',
    ward_code = '02287',
    address_detail = 'Số 262, Tổ Vĩnh Tài',
    latitude = '22.1430800',
    longitude = '105.2734300',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 66 AND code = 'KH0066';

-- Store ID 69: KH0069 - Điện Máy Bảo Anh TQ
UPDATE stores SET
    name = 'Điện Máy Bảo Anh TQ',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '08',
    district_code = '074',
    ward_code = '02374',
    address_detail = 'Số 226, Đường Nguyễn Trãi',
    latitude = '22.0390050',
    longitude = '105.0383450',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 69 AND code = 'KH0069';

-- Store ID 70: KH0070 - Điện Máy Ngọc Hiển TN
UPDATE stores SET
    name = 'Điện Máy Ngọc Hiển TN',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '19',
    district_code = '172',
    ward_code = '05860',
    address_detail = 'Số 301, Đường Trường Chinh',
    latitude = '21.4126980',
    longitude = '105.8724910',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 70 AND code = 'KH0070';

-- Store ID 71: KH0071 - Điện Máy Trung Xuân TN
UPDATE stores SET
    name = 'Điện Máy Trung Xuân TN',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '19',
    district_code = '164',
    ward_code = '05449',
    address_detail = 'Số 281, Đường Phùng Chí Kiên',
    latitude = '21.5961300',
    longitude = '105.8490320',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 71 AND code = 'KH0071';

-- Store ID 72: KH0072 - Điện Máy Gia Huy TN
UPDATE stores SET
    name = 'Điện Máy Gia Huy TN',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '19',
    district_code = '171',
    ward_code = '05821',
    address_detail = 'Số 93, Đường Đi La Hang',
    latitude = '21.6159110',
    longitude = '105.6169190',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 72 AND code = 'KH0072';

-- Store ID 73: KH0073 - Điện Máy Dũng Lam TN
UPDATE stores SET
    name = 'Điện Máy Dũng Lam TN',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '19',
    district_code = '172',
    ward_code = '05893',
    address_detail = 'Số 79, Đường Tôn Đức Thắng',
    latitude = '21.4189780',
    longitude = '105.8711660',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 73 AND code = 'KH0073';

-- Store ID 74: KH0074 - Điện Máy Trâm Hằng TN
UPDATE stores SET
    name = 'Điện Máy Trâm Hằng TN',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '20',
    district_code = '181',
    ward_code = '06091',
    address_detail = 'Số 113, Đường Nguyễn Trãi',
    latitude = '22.0625223',
    longitude = '106.2858604',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 74 AND code = 'KH0074';

-- Store ID 75: KH0075 - Điện Máy Hòa Dương TN
UPDATE stores SET
    name = 'Điện Máy Hòa Dương TN',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '19',
    district_code = '164',
    ward_code = '05446',
    address_detail = 'Số 315, Đường Quang Trung',
    latitude = '21.5826390',
    longitude = '105.8193350',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 75 AND code = 'KH0075';

-- Store ID 76: KH0076 - SC  Tùng Thành
UPDATE stores SET
    name = 'SC  Tùng Thành',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '20',
    district_code = '187',
    ward_code = '06463',
    address_detail = 'Số 254, Đường Ngô Quyền',
    latitude = '21.6140780',
    longitude = '106.5403760',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 76 AND code = 'KH0076';

-- Store ID 77: KH0077 - Tổng Kho Điện Máy Gia Hân TN
UPDATE stores SET
    name = 'Tổng Kho Điện Máy Gia Hân TN',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '19',
    district_code = '165',
    ward_code = '05521',
    address_detail = 'Số 97, Đường CMT8',
    latitude = '21.4629540',
    longitude = '105.8487480',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 77 AND code = 'KH0077';

-- Store ID 78: KH0079 - Cửa hàng Hưng Hưởng TN
UPDATE stores SET
    name = 'Cửa hàng Hưng Hưởng TN',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '19',
    district_code = '171',
    ward_code = '05836',
    address_detail = 'Số 181, Đường Phan Đình Phùng',
    latitude = '21.5816440',
    longitude = '105.5954940',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 78 AND code = 'KH0079';

-- Store ID 80: KH0081 - Điện Máy  Xuân Thọ TN
UPDATE stores SET
    name = 'Điện Máy  Xuân Thọ TN',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '19',
    district_code = '173',
    ward_code = '05908',
    address_detail = 'Số 235, Đường Hùng Vương',
    latitude = '21.4722540',
    longitude = '105.9534620',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 80 AND code = 'KH0081';

-- Store ID 81: KH0083 - Điện Máy Tuyết Khải BK
UPDATE stores SET
    name = 'Điện Máy Tuyết Khải BK',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '06',
    district_code = '062',
    ward_code = '01936',
    address_detail = 'Số 187, Đường Lý Thường Kiệt',
    latitude = '22.2905650',
    longitude = '105.8495260',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 81 AND code = 'KH0083';

-- Store ID 82: KH0084 - Điện Máy Hoa Lợi BK
UPDATE stores SET
    name = 'Điện Máy Hoa Lợi BK',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '06',
    district_code = '061',
    ward_code = '01888',
    address_detail = 'Số 166, Đường Lê Lợi',
    latitude = '22.2756640',
    longitude = '105.8764710',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 82 AND code = 'KH0084';

-- Store ID 84: KH0086 - Điện Máy Thanh Điệp BK ( Thành Điệp BC )
UPDATE stores SET
    name = 'Điện Máy Thanh Điệp BK ( Thành Điệp BC )',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '06',
    district_code = '060',
    ward_code = '01858',
    address_detail = 'Số 224, Đường Nguyễn Trãi',
    latitude = '22.2449110',
    longitude = '105.8759080',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 84 AND code = 'KH0086';

-- Store ID 107: KH0109 - Điện Máy Tiến Hợi  NB
UPDATE stores SET
    name = 'Điện Máy Tiến Hợi  NB',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '37',
    district_code = '377',
    ward_code = '14740',
    address_detail = 'Số 110, Xóm 3',
    latitude = '20.1281337',
    longitude = '106.0122765',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 107 AND code = 'KH0109';

-- Store ID 108: KH0110 - Điện Máy Hoàng Anh Yên Mô Ninh Bình
UPDATE stores SET
    name = 'Điện Máy Hoàng Anh Yên Mô Ninh Bình',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '37',
    district_code = '375',
    ward_code = '14599',
    address_detail = 'Số 107, Đường Bà Triệu, Xóm Đê',
    latitude = '20.1897243',
    longitude = '106.0946872',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 108 AND code = 'KH0110';

-- Store ID 109: KH0111 - Điện Máy Hoàng Tùng Ninh Bình
UPDATE stores SET
    name = 'Điện Máy Hoàng Tùng Ninh Bình',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '37',
    district_code = '372',
    ward_code = '14416',
    address_detail = 'Số 132, Đường Nguyễn Văn Hoan',
    latitude = '20.3115449',
    longitude = '105.7485879',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 109 AND code = 'KH0111';

-- Store ID 110: KH0112 - Điện Máy Nam Hương NB
UPDATE stores SET
    name = 'Điện Máy Nam Hương NB',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '37',
    district_code = '377',
    ward_code = '14728',
    address_detail = 'Số 218, Đường Hùng Vương',
    latitude = '20.1309260',
    longitude = '106.0259952',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 110 AND code = 'KH0112';

-- Store ID 111: KH0113 - Điện máy Sơn Thảo
UPDATE stores SET
    name = 'Điện máy Sơn Thảo',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '37',
    district_code = '374',
    ward_code = '14320',
    address_detail = 'Số 60, Đường Lê Lợi',
    latitude = '20.2953540',
    longitude = '105.9508490',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 111 AND code = 'KH0113';

-- Store ID 112: KH0114 - Điện máy Trường Hiền HP
UPDATE stores SET
    name = 'Điện máy Trường Hiền HP',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '31',
    district_code = '311',
    ward_code = '11488',
    address_detail = 'Số 279, Đường Bà Triệu',
    latitude = '20.9650550',
    longitude = '106.6738440',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 112 AND code = 'KH0114';

-- Store ID 113: KH0115 - Điện Máy Sơn Hà HP
UPDATE stores SET
    name = 'Điện Máy Sơn Hà HP',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '31',
    district_code = '315',
    ward_code = '11755',
    address_detail = 'Số 274, Đường Quang Trung',
    latitude = '20.7257310',
    longitude = '106.5531740',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 113 AND code = 'KH0115';

-- Store ID 114: KH0116 - Điện Máy Quảng Thi HP
UPDATE stores SET
    name = 'Điện Máy Quảng Thi HP',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '31',
    district_code = '313',
    ward_code = '11632',
    address_detail = 'Số 207, Bát Trang',
    latitude = '20.8498580',
    longitude = '106.5124290',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 114 AND code = 'KH0116';

-- Store ID 115: KH0117 - Điện máy Tuyên Loan
UPDATE stores SET
    name = 'Điện máy Tuyên Loan',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '31',
    district_code = '315',
    ward_code = '11755',
    address_detail = 'Số 139, Khu 4',
    latitude = '20.7306440',
    longitude = '106.5614000',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 115 AND code = 'KH0117';

-- Store ID 116: KH0118 - Điện Máy Thủy Sản GL
UPDATE stores SET
    name = 'Điện Máy Thủy Sản GL',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '30',
    district_code = '288',
    ward_code = '10507',
    address_detail = 'Số 302, Phố Đinh Đàm',
    latitude = '20.9505280',
    longitude = '106.3185320',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 116 AND code = 'KH0118';

-- Store ID 117: KH0119 - Điện Máy Đỗ Thắng HD
UPDATE stores SET
    name = 'Điện Máy Đỗ Thắng HD',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '30',
    district_code = '296',
    ward_code = '10945',
    address_detail = 'Số 16, Đường Thống Nhất',
    latitude = '20.9089110',
    longitude = '106.1475410',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 117 AND code = 'KH0119';

-- Store ID 118: KH0120 - Điện Máy Hoàng Khoát HP
UPDATE stores SET
    name = 'Điện Máy Hoàng Khoát HP',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '31',
    district_code = '311',
    ward_code = '11557',
    address_detail = 'Số 31, Đồng Giá',
    latitude = '20.9191080',
    longitude = '106.6527240',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 118 AND code = 'KH0120';

-- Store ID 119: KH0121 - Điện Máy Nhật Bản HD
UPDATE stores SET
    name = 'Điện Máy Nhật Bản HD',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '30',
    district_code = '298',
    ward_code = '11074',
    address_detail = 'Số 320, Đường Lý Thường Kiệt',
    latitude = '20.8242070',
    longitude = '106.4008480',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 119 AND code = 'KH0121';

-- Store ID 120: KH0122 - Trung Tâm Điện Máy Hưng Thúy HD
UPDATE stores SET
    name = 'Trung Tâm Điện Máy Hưng Thúy HD',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '30',
    district_code = '293',
    ward_code = '10750',
    address_detail = 'Số 70, Phố Hồng Thái',
    latitude = '20.9636680',
    longitude = '106.5131960',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 120 AND code = 'KH0122';

-- Store ID 121: KH0123 - Điện Máy Tấn Tài HD
UPDATE stores SET
    name = 'Điện Máy Tấn Tài HD',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '30',
    district_code = '297',
    ward_code = '11044',
    address_detail = 'Số 109, Đường Trục Bắc Nam',
    latitude = '20.8473030',
    longitude = '106.2925460',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 121 AND code = 'KH0123';

-- Store ID 122: KH0124 - Điện Tử - Điện Lạnh Nhật Minh HP
UPDATE stores SET
    name = 'Điện Tử - Điện Lạnh Nhật Minh HP',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '31',
    district_code = '313',
    ward_code = '11629',
    address_detail = 'Số 166, Đường Nguyễn Văn Trỗi',
    latitude = '20.8219220',
    longitude = '106.5562500',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 122 AND code = 'KH0124';

-- Store ID 123: KH0125 - Điện Máy Tấn Tài HP
UPDATE stores SET
    name = 'Điện Máy Tấn Tài HP',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '31',
    district_code = '315',
    ward_code = '11755',
    address_detail = 'Số 40, Khu 4',
    latitude = '20.7291080',
    longitude = '106.5600160',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 123 AND code = 'KH0125';

-- Store ID 124: KH0126 - Điện Máy Ngọc Hoan HY
UPDATE stores SET
    name = 'Điện Máy Ngọc Hoan HY',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '33',
    district_code = '325',
    ward_code = '11986',
    address_detail = 'Số 146, Đường Quảng Trường',
    latitude = '20.9779410',
    longitude = '105.9917530',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 124 AND code = 'KH0126';

-- Store ID 125: KH0127 - Điện Máy Công Bằng HY
UPDATE stores SET
    name = 'Điện Máy Công Bằng HY',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '33',
    district_code = '330',
    ward_code = '12205',
    address_detail = 'Số 289, Đường Nguyễn Kỳ',
    latitude = '20.8374879',
    longitude = '105.9784480',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 125 AND code = 'KH0127';

-- Store ID 126: KH0128 - Điện Máy Anh Vân HY
UPDATE stores SET
    name = 'Điện Máy Anh Vân HY',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '33',
    district_code = '329',
    ward_code = '12169',
    address_detail = 'Số 183, Thôn Đỗ Xuyên',
    latitude = '20.8217660',
    longitude = '106.1098830',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 126 AND code = 'KH0128';

-- Store ID 127: KH0129 - Trung Tâm Điện Máy  Bảo Làn HY
UPDATE stores SET
    name = 'Trung Tâm Điện Máy  Bảo Làn HY',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '33',
    district_code = '331',
    ward_code = '12304',
    address_detail = 'Số 187, Đường Tô Hiệu',
    latitude = '20.7492760',
    longitude = '106.0665550',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 127 AND code = 'KH0129';

-- Store ID 128: KH0130 - Đại Lý Hưng thủy HY
UPDATE stores SET
    name = 'Đại Lý Hưng thủy HY',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '33',
    district_code = '333',
    ward_code = '12412',
    address_detail = 'Số 334, Đình Cao',
    latitude = '20.7052900',
    longitude = '106.1856760',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 128 AND code = 'KH0130';

-- Store ID 129: KH0131 - Điện Máy Xuân Hải HY
UPDATE stores SET
    name = 'Điện Máy Xuân Hải HY',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '33',
    district_code = '330',
    ward_code = '12205',
    address_detail = 'Số 180, Đường Nguyễn Trãi',
    latitude = '20.8402802',
    longitude = '105.9921667',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 129 AND code = 'KH0131';

-- Store ID 130: KH0132 - Điện Máy Bình Hạnh HY
UPDATE stores SET
    name = 'Điện Máy Bình Hạnh HY',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '33',
    district_code = '323',
    ward_code = '11956',
    address_detail = 'Số 10, Đường Phan Đình Phùng',
    latitude = '20.6478787',
    longitude = '106.0497759',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 130 AND code = 'KH0132';

-- Store ID 131: KH0133 - Điện Tử Hằng Trường
UPDATE stores SET
    name = 'Điện Tử Hằng Trường',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '33',
    district_code = '331',
    ward_code = '12304',
    address_detail = 'Số 84, Tạ Hạ',
    latitude = '20.7511160',
    longitude = '106.0729110',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 131 AND code = 'KH0133';

-- Store ID 132: KH0134 - Điện Máy Trường Viên QN
UPDATE stores SET
    name = 'Điện Máy Trường Viên QN',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '22',
    district_code = '200',
    ward_code = '06895',
    address_detail = 'Số 138, Đường Lê Lương',
    latitude = '21.3506650',
    longitude = '107.5971890',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 132 AND code = 'KH0134';

-- Store ID 133: KH0135 - Điện Máy Hưng Kiều QN
UPDATE stores SET
    name = 'Điện Máy Hưng Kiều QN',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '22',
    district_code = '199',
    ward_code = '06862',
    address_detail = 'Số 15, Đường Hòa Bình',
    latitude = '21.3325780',
    longitude = '107.4038230',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 133 AND code = 'KH0135';

-- Store ID 134: KH0136 - Điện Máy Minh Quyết QN
UPDATE stores SET
    name = 'Điện Máy Minh Quyết QN',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '22',
    district_code = '200',
    ward_code = '06895',
    address_detail = 'Số 292, Đường Hoàng Văn Thụ',
    latitude = '21.3548520',
    longitude = '107.5898670',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 134 AND code = 'KH0136';

-- Store ID 136: KH0138 - Điện Máy Tuyết Trụ
UPDATE stores SET
    name = 'Điện Máy Tuyết Trụ',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '01',
    district_code = '008',
    ward_code = '00340',
    address_detail = 'Số 272, Đường KĐT Đồng Tàu',
    latitude = '20.9753920',
    longitude = '105.8573399',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 136 AND code = 'KH0138';

-- Store ID 137: KH0139 - Điện máy Thành Tuyến Sơn Tây
UPDATE stores SET
    name = 'Điện máy Thành Tuyến Sơn Tây',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '01',
    district_code = '269',
    ward_code = '09586',
    address_detail = 'Ngõ 17, Phố Chùa Thông',
    latitude = '21.1210630',
    longitude = '105.4991760',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 137 AND code = 'KH0139';

-- Store ID 138: KH0140 - Điện Máy Thiên Phú
UPDATE stores SET
    name = 'Điện Máy Thiên Phú',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '01',
    district_code = '276',
    ward_code = '10009',
    address_detail = '99/Số 3, Thôn 1',
    latitude = '21.0194770',
    longitude = '105.5651150',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 138 AND code = 'KH0140';

-- Store ID 139: KH0141 - Điện máy Kiên Cường
UPDATE stores SET
    name = 'Điện máy Kiên Cường',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '01',
    district_code = '271',
    ward_code = '09709',
    address_detail = 'Số 140, Đường Đá Bạc',
    latitude = '21.1055650',
    longitude = '105.4254310',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 139 AND code = 'KH0141';

-- Store ID 140: KH0142 - Điện máy Tuấn Sơn CS2 BT
UPDATE stores SET
    name = 'Điện máy Tuấn Sơn CS2 BT',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '01',
    district_code = '282',
    ward_code = '10441',
    address_detail = 'Số 203, Đường Nguyễn Huệ',
    latitude = '20.6866420',
    longitude = '105.7433320',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 140 AND code = 'KH0142';

-- Store ID 141: KH0143 - Điện Máy Nghĩa Huệ BT
UPDATE stores SET
    name = 'Điện Máy Nghĩa Huệ BT',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '01',
    district_code = '282',
    ward_code = '10477',
    address_detail = 'Số 251, Thôn 2',
    latitude = '20.7014360',
    longitude = '105.7578100',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 141 AND code = 'KH0143';

-- Store ID 142: KH0144 - Điện Máy Tân Hường
UPDATE stores SET
    name = 'Điện Máy Tân Hường',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '01',
    district_code = '275',
    ward_code = '09895',
    address_detail = 'Số 308, Đường Phố Huyện',
    latitude = '20.9903550',
    longitude = '105.6467620',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 142 AND code = 'KH0144';

-- Store ID 143: KH0145 - Điện Máy Kiều Trụ BT
UPDATE stores SET
    name = 'Điện Máy Kiều Trụ BT',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '01',
    district_code = '278',
    ward_code = '10159',
    address_detail = 'Số 160, Đường Phạm Hùng',
    latitude = '20.8407840',
    longitude = '105.7637220',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 143 AND code = 'KH0145';

-- Store ID 144: KH0146 - Điện Máy  Thành Đạt BT
UPDATE stores SET
    name = 'Điện Máy  Thành Đạt BT',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '01',
    district_code = '277',
    ward_code = '10015',
    address_detail = 'Số 140, Đường Đình Ngọc Giả',
    latitude = '20.9232920',
    longitude = '105.6988660',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 144 AND code = 'KH0146';

-- Store ID 145: KH0147 - Điện Máy Bính Vân
UPDATE stores SET
    name = 'Điện Máy Bính Vân',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '01',
    district_code = '276',
    ward_code = '10009',
    address_detail = 'Số 73, Đường Hạ Bằng',
    latitude = '21.0093260',
    longitude = '105.5568060',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 145 AND code = 'KH0147';

-- Store ID 146: KH0148 - Điện Máy Huy Khánh BT 2
UPDATE stores SET
    name = 'Điện Máy Huy Khánh BT 2',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '01',
    district_code = '282',
    ward_code = '10477',
    address_detail = 'Số 136, Thôn 6',
    latitude = '20.6963340',
    longitude = '105.7442520',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 146 AND code = 'KH0148';

-- Store ID 147: KH0149 - Điện Máy  Yên Phi ST
UPDATE stores SET
    name = 'Điện Máy  Yên Phi ST',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '01',
    district_code = '271',
    ward_code = '09694',
    address_detail = 'Số 117, Đường Tản Lĩnh',
    latitude = '21.1147380',
    longitude = '105.4037640',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 147 AND code = 'KH0149';

-- Store ID 148: KH0150 - Điện Máy  Đức Huỳnh BT
UPDATE stores SET
    name = 'Điện Máy  Đức Huỳnh BT',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '01',
    district_code = '277',
    ward_code = '10015',
    address_detail = 'Số 164, Đường Hòa Sơn',
    latitude = '20.9236630',
    longitude = '105.7038660',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 148 AND code = 'KH0150';

-- Store ID 149: KH0151 - Điện Máy Thanh Bình BT
UPDATE stores SET
    name = 'Điện Máy Thanh Bình BT',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '01',
    district_code = '006',
    ward_code = '00178',
    address_detail = 'Số 18, Phố Tây Sơn',
    latitude = '21.0042580',
    longitude = '105.8214110',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 149 AND code = 'KH0151';

-- Store ID 150: KH0152 - Điện Máy Vịnh Hảo
UPDATE stores SET
    name = 'Điện Máy Vịnh Hảo',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '01',
    district_code = '276',
    ward_code = '09958',
    address_detail = 'Số 268, Đường Đại Đồng',
    latitude = '21.0836330',
    longitude = '105.5667780',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 150 AND code = 'KH0152';

-- Store ID 151: KH0153 - Điện Máy Tâm Thủy (Thủy Khôi ) NDP
UPDATE stores SET
    name = 'Điện Máy Tâm Thủy (Thủy Khôi ) NDP',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '36',
    district_code = '361',
    ward_code = NULL,
    address_detail = 'Số 9, Đường Lý Thường Kiệt, Huyện Nghĩa Hưng, Tỉnh Nam Định',
    latitude = '20.1680000',
    longitude = '106.1830000',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 151 AND code = 'KH0153';

-- Store ID 152: KH0154 - Điện Máy Minh Khôi NĐH
UPDATE stores SET
    name = 'Điện Máy Minh Khôi NĐH',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '36',
    district_code = '360',
    ward_code = NULL,
    address_detail = 'Số 310, Đường Hùng Vương, Huyện ý Yên, Tỉnh Nam Định',
    latitude = '20.3312000',
    longitude = '106.0125000',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 152 AND code = 'KH0154';

-- Store ID 153: KH0155 - Điện Máy Thúy Xiêm (Xiêm Thúy ) NDH
UPDATE stores SET
    name = 'Điện Máy Thúy Xiêm (Xiêm Thúy ) NDH',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '36',
    district_code = '359',
    ward_code = NULL,
    address_detail = 'Số 102, Đường Hùng Vương, Huyện Vụ Bản, Tỉnh Nam Định',
    latitude = '20.3750000',
    longitude = '106.0833000',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 153 AND code = 'KH0155';

-- Store ID 154: KH0156 - Điện Máy Hùng nguyệt NDH
UPDATE stores SET
    name = 'Điện Máy Hùng nguyệt NDH',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '36',
    district_code = '364',
    ward_code = NULL,
    address_detail = 'Số 274, Đường Hoàng Văn Thụ, Huyện Xuân Trường, Tỉnh Nam Định',
    latitude = '20.2850000',
    longitude = '106.3520000',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 154 AND code = 'KH0156';

-- Store ID 155: KH0157 - Điện Tử - Điện Lạnh Thắng Long NĐH
UPDATE stores SET
    name = 'Điện Tử - Điện Lạnh Thắng Long NĐH',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '35',
    district_code = '353',
    ward_code = NULL,
    address_detail = 'Số 274, Đường Lê Lợi, Huyện Lý Nhân, Tỉnh Hà Nam',
    latitude = '20.5340294',
    longitude = '105.9810248',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 155 AND code = 'KH0157';

-- Store ID 156: KH0158 - Điện Máy Kiên Thúy HN
UPDATE stores SET
    name = 'Điện Máy Kiên Thúy HN',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '35',
    district_code = '349',
    ward_code = NULL,
    address_detail = 'Số 163, Đường Hoàng Văn Thụ, Huyện Duy Tiên, Tỉnh Hà Nam',
    latitude = '20.5368217',
    longitude = '105.9947435',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 156 AND code = 'KH0158';

-- Store ID 157: KH0159 - Điện Máy Quân Huệ NDH
UPDATE stores SET
    name = 'Điện Máy Quân Huệ NDH',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '35',
    district_code = '347',
    ward_code = NULL,
    address_detail = 'Số 61, Đường Ngô Quyền, TP Phủ Lý, Tỉnh Hà Nam',
    latitude = '20.5445676',
    longitude = '105.9339242',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 157 AND code = 'KH0159';

-- Store ID 158: KH0160 - Cửa hàng  Hồng Sơn NDH
UPDATE stores SET
    name = 'Cửa hàng  Hồng Sơn NDH',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '35',
    district_code = '353',
    ward_code = NULL,
    address_detail = 'Số 44, Đường Lê Lợi, Huyện Lý Nhân, Tỉnh Hà Nam',
    latitude = '20.5156206',
    longitude = '105.9888425',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 158 AND code = 'KH0160';

-- Store ID 159: KH0161 - Điện Máy Đức Tài  NDH
UPDATE stores SET
    name = 'Điện Máy Đức Tài  NDH',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '35',
    district_code = '349',
    ward_code = NULL,
    address_detail = 'Số 310, Đường Trường Chinh, Huyện Duy Tiên, Tỉnh Hà Nam',
    latitude = '20.5192975',
    longitude = '105.9596012',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 159 AND code = 'KH0161';

-- Store ID 160: KH0162 - Điện Máy Hoàng Anh NDH
UPDATE stores SET
    name = 'Điện Máy Hoàng Anh NDH',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '36',
    district_code = '362',
    ward_code = NULL,
    address_detail = 'Số 115, Đường Bà Triệu, Huyện Nam Trực, Tỉnh Nam Định',
    latitude = '20.3500000',
    longitude = '106.2167000',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 160 AND code = 'KH0162';

-- Store ID 161: KH0163 - Điện Máy Trường Phượng NDH
UPDATE stores SET
    name = 'Điện Máy Trường Phượng NDH',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '35',
    district_code = '352',
    ward_code = NULL,
    address_detail = 'Số 16, Đường Hùng Vương, Huyện Bình Lục, Tỉnh Hà Nam',
    latitude = '20.5562508',
    longitude = '105.9579985',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 161 AND code = 'KH0163';

-- Store ID 162: KH0164 - Điện Máy  Hòa Hạnh NDH
UPDATE stores SET
    name = 'Điện Máy  Hòa Hạnh NDH',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '35',
    district_code = '351',
    ward_code = NULL,
    address_detail = 'Số 41, Đường Lý Thường Kiệt, Huyện Thanh Liêm, Tỉnh Hà Nam',
    latitude = '20.5660868',
    longitude = '106.0014288',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 162 AND code = 'KH0164';

-- Store ID 163: KH0165 - Điện máy Nam Anh NĐH
UPDATE stores SET
    name = 'Điện máy Nam Anh NĐH',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '36',
    district_code = '363',
    ward_code = NULL,
    address_detail = 'Số 118, Đường Bà Triệu, Huyện Trực Ninh, Tỉnh Nam Định',
    latitude = '20.2500000',
    longitude = '106.2333000',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 163 AND code = 'KH0165';

-- Store ID 164: KH0166 - Điện Máy Dưỡng Hoài NDH
UPDATE stores SET
    name = 'Điện Máy Dưỡng Hoài NDH',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '36',
    district_code = '356',
    ward_code = NULL,
    address_detail = 'Số 107, Đường Nguyễn Trãi, TP Nam Định, Tỉnh Nam Định',
    latitude = '20.4387624',
    longitude = '106.1752638',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 164 AND code = 'KH0166';

-- Store ID 165: KH0167 - Trung Tâm Điện Tử Điện Lạnh Nghị Nhì TB(  Bác Nghị )
UPDATE stores SET
    name = 'Trung Tâm Điện Tử Điện Lạnh Nghị Nhì TB(  Bác Nghị )',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '34',
    district_code = '341',
    ward_code = '12907',
    address_detail = 'Số 22, Đường Trần Hưng Đạo',
    latitude = '20.5656720',
    longitude = '106.5650860',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 165 AND code = 'KH0167';

-- Store ID 166: KH0168 - Điện Máy Thanh Phong TB
UPDATE stores SET
    name = 'Điện Máy Thanh Phong TB',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '34',
    district_code = '336',
    ward_code = '12436',
    address_detail = 'Số 201, Đường Lê Quý Đôn',
    latitude = '20.4541910',
    longitude = '106.3387880',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 166 AND code = 'KH0168';

-- Store ID 167: KH0169 - Điện Máy ánh Chinh TB
UPDATE stores SET
    name = 'Điện Máy ánh Chinh TB',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '34',
    district_code = '338',
    ward_code = '12472',
    address_detail = 'Số 161, Đường Trường Chinh',
    latitude = '20.6438530',
    longitude = '106.3303710',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 167 AND code = 'KH0169';

-- Store ID 168: KH0170 - Sửa Chữa Hiệp Huệ TB
UPDATE stores SET
    name = 'Sửa Chữa Hiệp Huệ TB',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '34',
    district_code = '342',
    ward_code = '12970',
    address_detail = 'Số 259, Đường Ngô Quang Đoan',
    latitude = '20.4002650',
    longitude = '106.4983840',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 168 AND code = 'KH0170';

-- Store ID 169: KH0171 - Điện Máy Đức Tin TB
UPDATE stores SET
    name = 'Điện Máy Đức Tin TB',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '34',
    district_code = '336',
    ward_code = '12436',
    address_detail = 'Số 253, Đường Lê Quý Đôn',
    latitude = '20.4528840',
    longitude = '106.3397960',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 169 AND code = 'KH0171';

-- Store ID 170: KH0172 - Điện máy Quang Thành TB
UPDATE stores SET
    name = 'Điện máy Quang Thành TB',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '34',
    district_code = '342',
    ward_code = '13024',
    address_detail = 'Số 105, Đường Lê Lợi',
    latitude = '20.3917950',
    longitude = '106.5458350',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 170 AND code = 'KH0172';

-- Store ID 171: KH0173 - Điện Máy Văn Vân TB
UPDATE stores SET
    name = 'Điện Máy Văn Vân TB',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '34',
    district_code = '344',
    ward_code = '13192',
    address_detail = 'Số 25, Đường Hoàng Văn Thụ',
    latitude = '20.4490626',
    longitude = '106.2988532',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 171 AND code = 'KH0173';

-- Store ID 172: KH0174 - Điện Máy Huyền Trung TB
UPDATE stores SET
    name = 'Điện Máy Huyền Trung TB',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '34',
    district_code = '336',
    ward_code = '12433',
    address_detail = 'Số 194, Đường Trần Lãm',
    latitude = '20.4344760',
    longitude = '106.3478407',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 172 AND code = 'KH0174';

-- Store ID 173: KH0177 - Trung Tâm Điện Máy Quang Thiều NB
UPDATE stores SET
    name = 'Trung Tâm Điện Máy Quang Thiều NB',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '37',
    district_code = '376',
    ward_code = '14677',
    address_detail = 'Số 211, Đường Phan Đình Phùng',
    latitude = '20.0240516',
    longitude = '106.0879467',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 173 AND code = 'KH0177';

-- Store ID 174: KH0198 - Công Ty TNHH Hiếu Phát HN
UPDATE stores SET
    name = 'Công Ty TNHH Hiếu Phát HN',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '01',
    district_code = '017',
    ward_code = '00454',
    address_detail = 'Số 212, Đường Cao Lỗ',
    latitude = '21.1405580',
    longitude = '105.8466733',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 174 AND code = 'KH0198';

-- Store ID 175: KH0199 - Điện Máy Đạo Uyên ĐA
UPDATE stores SET
    name = 'Điện Máy Đạo Uyên ĐA',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '01',
    district_code = '008',
    ward_code = '00334',
    address_detail = 'Số 79, Đường Trần Phú',
    latitude = '20.9687760',
    longitude = '105.8803940',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = NULL,
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 175 AND code = 'KH0199';

-- Store ID 176: KH0200 - Công ty CP Quốc tế WinWin HN
UPDATE stores SET
    name = 'Công ty CP Quốc tế WinWin HN',
    contact_name = NULL,
    contact_phone = NULL,
    province_code = '01',
    district_code = '016',
    ward_code = '00376',
    address_detail = 'Số 82, Đường Lý Thường Kiệt',
    latitude = '21.2573860',
    longitude = '105.8466460',
    time_window_start = NULL,
    time_window_end = NULL,
    allowed_delivery_hours = 'All',
    max_allowed_vehicle_weight = '0.000',
    restricted_vehicle_types = NULL,
    is_active = TRUE
WHERE id = 176 AND code = 'KH0200';

-- --------------------------------------------------------------------------
-- 3. ROUTE STOPS SEQUENCE ORDER
-- Shift sequence_order to avoid unique constraint (uq_route_stops_seq) conflict
-- --------------------------------------------------------------------------
UPDATE route_stops SET sequence_order = sequence_order + 1000 WHERE route_id IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

-- Sequence for Route ID 1 (11 stops)
UPDATE route_stops SET sequence_order = 1 WHERE route_id = 1 AND store_id = 175; -- KH0199 - Điện Máy Đạo Uyên ĐA
UPDATE route_stops SET sequence_order = 2 WHERE route_id = 1 AND store_id = 136; -- KH0138 - Điện Máy Tuyết Trụ
UPDATE route_stops SET sequence_order = 3 WHERE route_id = 1 AND store_id = 142; -- KH0144 - Điện Máy Tân Hường
UPDATE route_stops SET sequence_order = 4 WHERE route_id = 1 AND store_id = 138; -- KH0140 - Điện Máy Thiên Phú
UPDATE route_stops SET sequence_order = 5 WHERE route_id = 1 AND store_id = 145; -- KH0147 - Điện Máy Bính Vân
UPDATE route_stops SET sequence_order = 6 WHERE route_id = 1 AND store_id = 150; -- KH0152 - Điện Máy Vịnh Hảo
UPDATE route_stops SET sequence_order = 7 WHERE route_id = 1 AND store_id = 137; -- KH0139 - Điện máy Thành Tuyến Sơn Tây
UPDATE route_stops SET sequence_order = 8 WHERE route_id = 1 AND store_id = 139; -- KH0141 - Điện máy Kiên Cường
UPDATE route_stops SET sequence_order = 9 WHERE route_id = 1 AND store_id = 147; -- KH0149 - Điện Máy  Yên Phi ST
UPDATE route_stops SET sequence_order = 10 WHERE route_id = 1 AND store_id = 176; -- KH0200 - Công ty CP Quốc tế WinWin HN
UPDATE route_stops SET sequence_order = 11 WHERE route_id = 1 AND store_id = 174; -- KH0198 - Công Ty TNHH Hiếu Phát HN

-- Sequence for Route ID 2 (14 stops)
UPDATE route_stops SET sequence_order = 1 WHERE route_id = 2 AND store_id = 162; -- KH0164 - Điện Máy  Hòa Hạnh NDH
UPDATE route_stops SET sequence_order = 2 WHERE route_id = 2 AND store_id = 161; -- KH0163 - Điện Máy Trường Phượng NDH
UPDATE route_stops SET sequence_order = 3 WHERE route_id = 2 AND store_id = 157; -- KH0159 - Điện Máy Quân Huệ NDH
UPDATE route_stops SET sequence_order = 4 WHERE route_id = 2 AND store_id = 159; -- KH0161 - Điện Máy Đức Tài  NDH
UPDATE route_stops SET sequence_order = 5 WHERE route_id = 2 AND store_id = 155; -- KH0157 - Điện Tử - Điện Lạnh Thắng Long NĐH
UPDATE route_stops SET sequence_order = 6 WHERE route_id = 2 AND store_id = 156; -- KH0158 - Điện Máy Kiên Thúy HN
UPDATE route_stops SET sequence_order = 7 WHERE route_id = 2 AND store_id = 158; -- KH0160 - Cửa hàng  Hồng Sơn NDH
UPDATE route_stops SET sequence_order = 8 WHERE route_id = 2 AND store_id = 152; -- KH0154 - Điện Máy Minh Khôi NĐH
UPDATE route_stops SET sequence_order = 9 WHERE route_id = 2 AND store_id = 153; -- KH0155 - Điện Máy Thúy Xiêm (Xiêm Thúy ) NDH
UPDATE route_stops SET sequence_order = 10 WHERE route_id = 2 AND store_id = 164; -- KH0166 - Điện Máy Dưỡng Hoài NDH
UPDATE route_stops SET sequence_order = 11 WHERE route_id = 2 AND store_id = 160; -- KH0162 - Điện Máy Hoàng Anh NDH
UPDATE route_stops SET sequence_order = 12 WHERE route_id = 2 AND store_id = 163; -- KH0165 - Điện máy Nam Anh NĐH
UPDATE route_stops SET sequence_order = 13 WHERE route_id = 2 AND store_id = 151; -- KH0153 - Điện Máy Tâm Thủy (Thủy Khôi ) NDP
UPDATE route_stops SET sequence_order = 14 WHERE route_id = 2 AND store_id = 154; -- KH0156 - Điện Máy Hùng nguyệt NDH

-- Sequence for Route ID 3 (10 stops)
UPDATE route_stops SET sequence_order = 1 WHERE route_id = 3 AND store_id = 111; -- KH0113 - Điện máy Sơn Thảo
UPDATE route_stops SET sequence_order = 2 WHERE route_id = 3 AND store_id = 108; -- KH0110 - Điện Máy Hoàng Anh Yên Mô Ninh Bình
UPDATE route_stops SET sequence_order = 3 WHERE route_id = 3 AND store_id = 173; -- KH0177 - Trung Tâm Điện Máy Quang Thiều NB
UPDATE route_stops SET sequence_order = 4 WHERE route_id = 3 AND store_id = 110; -- KH0112 - Điện Máy Nam Hương NB
UPDATE route_stops SET sequence_order = 5 WHERE route_id = 3 AND store_id = 107; -- KH0109 - Điện Máy Tiến Hợi  NB
UPDATE route_stops SET sequence_order = 6 WHERE route_id = 3 AND store_id = 109; -- KH0111 - Điện Máy Hoàng Tùng Ninh Bình
UPDATE route_stops SET sequence_order = 7 WHERE route_id = 3 AND store_id = 2; -- KH0002 - HỘ KINH DOANH PHẠM NHƯ DŨNG
UPDATE route_stops SET sequence_order = 8 WHERE route_id = 3 AND store_id = 3; -- KH0003 - Điện Máy Lâm Oanh BT
UPDATE route_stops SET sequence_order = 9 WHERE route_id = 3 AND store_id = 5; -- KH0005 - Điện máy Tất Thành
UPDATE route_stops SET sequence_order = 10 WHERE route_id = 3 AND store_id = 25; -- KH0025 - Điện Máy Ngọc Phong

-- Sequence for Route ID 4 (9 stops)
UPDATE route_stops SET sequence_order = 1 WHERE route_id = 4 AND store_id = 125; -- KH0127 - Điện Máy Công Bằng HY
UPDATE route_stops SET sequence_order = 2 WHERE route_id = 4 AND store_id = 129; -- KH0131 - Điện Máy Xuân Hải HY
UPDATE route_stops SET sequence_order = 3 WHERE route_id = 4 AND store_id = 127; -- KH0129 - Trung Tâm Điện Máy  Bảo Làn HY
UPDATE route_stops SET sequence_order = 4 WHERE route_id = 4 AND store_id = 131; -- KH0133 - Điện Tử Hằng Trường
UPDATE route_stops SET sequence_order = 5 WHERE route_id = 4 AND store_id = 130; -- KH0132 - Điện Máy Bình Hạnh HY
UPDATE route_stops SET sequence_order = 6 WHERE route_id = 4 AND store_id = 128; -- KH0130 - Đại Lý Hưng thủy HY
UPDATE route_stops SET sequence_order = 7 WHERE route_id = 4 AND store_id = 126; -- KH0128 - Điện Máy Anh Vân HY
UPDATE route_stops SET sequence_order = 8 WHERE route_id = 4 AND store_id = 117; -- KH0119 - Điện Máy Đỗ Thắng HD
UPDATE route_stops SET sequence_order = 9 WHERE route_id = 4 AND store_id = 124; -- KH0126 - Điện Máy Ngọc Hoan HY

-- Sequence for Route ID 5 (8 stops)
UPDATE route_stops SET sequence_order = 1 WHERE route_id = 5 AND store_id = 171; -- KH0173 - Điện Máy Văn Vân TB
UPDATE route_stops SET sequence_order = 2 WHERE route_id = 5 AND store_id = 166; -- KH0168 - Điện Máy Thanh Phong TB
UPDATE route_stops SET sequence_order = 3 WHERE route_id = 5 AND store_id = 169; -- KH0171 - Điện Máy Đức Tin TB
UPDATE route_stops SET sequence_order = 4 WHERE route_id = 5 AND store_id = 172; -- KH0174 - Điện Máy Huyền Trung TB
UPDATE route_stops SET sequence_order = 5 WHERE route_id = 5 AND store_id = 168; -- KH0170 - Sửa Chữa Hiệp Huệ TB
UPDATE route_stops SET sequence_order = 6 WHERE route_id = 5 AND store_id = 170; -- KH0172 - Điện máy Quang Thành TB
UPDATE route_stops SET sequence_order = 7 WHERE route_id = 5 AND store_id = 165; -- KH0167 - Trung Tâm Điện Tử Điện Lạnh Nghị Nhì TB(  Bác Nghị )
UPDATE route_stops SET sequence_order = 8 WHERE route_id = 5 AND store_id = 167; -- KH0169 - Điện Máy ánh Chinh TB

-- Sequence for Route ID 6 (11 stops)
UPDATE route_stops SET sequence_order = 1 WHERE route_id = 6 AND store_id = 45; -- KH0045 - Điện Máy Tân Tiến BN
UPDATE route_stops SET sequence_order = 2 WHERE route_id = 6 AND store_id = 50; -- KH0050 - Trung Tâm Điện Máy Tuấn Trang BG
UPDATE route_stops SET sequence_order = 3 WHERE route_id = 6 AND store_id = 42; -- KH0042 - Điện Máy Thá Phượng BN
UPDATE route_stops SET sequence_order = 4 WHERE route_id = 6 AND store_id = 46; -- KH0046 - Điện Máy Chiến Lan BN
UPDATE route_stops SET sequence_order = 5 WHERE route_id = 6 AND store_id = 40; -- KH0040 - Điện Máy  Điện Giang BG
UPDATE route_stops SET sequence_order = 6 WHERE route_id = 6 AND store_id = 41; -- KH0041 - Điện Máy Luyện Thanh BG
UPDATE route_stops SET sequence_order = 7 WHERE route_id = 6 AND store_id = 44; -- KH0044 - Điện Máy Duy Quang BG
UPDATE route_stops SET sequence_order = 8 WHERE route_id = 6 AND store_id = 43; -- KH0043 - Điện Máy Tuấn Hưng BG
UPDATE route_stops SET sequence_order = 9 WHERE route_id = 6 AND store_id = 133; -- KH0135 - Điện Máy Hưng Kiều QN
UPDATE route_stops SET sequence_order = 10 WHERE route_id = 6 AND store_id = 134; -- KH0136 - Điện Máy Minh Quyết QN
UPDATE route_stops SET sequence_order = 11 WHERE route_id = 6 AND store_id = 132; -- KH0134 - Điện Máy Trường Viên QN

-- Sequence for Route ID 7 (10 stops)
UPDATE route_stops SET sequence_order = 1 WHERE route_id = 7 AND store_id = 22; -- KH0022 - Điện Tử - Điện Lạnh - Điện Dân Dụng - Thiết Bị Âm Thanh Thanh Hạnh VP
UPDATE route_stops SET sequence_order = 2 WHERE route_id = 7 AND store_id = 23; -- KH0023 - Kim Khí Tổng Hợp  Phương Nam VP
UPDATE route_stops SET sequence_order = 3 WHERE route_id = 7 AND store_id = 19; -- KH0019 - Điện Máy Toàn Lan VP
UPDATE route_stops SET sequence_order = 4 WHERE route_id = 7 AND store_id = 20; -- KH0020 - Điện Máy Quỳnh Vân VP
UPDATE route_stops SET sequence_order = 5 WHERE route_id = 7 AND store_id = 21; -- KH0021 - Hoàng Thị Việt Hà -Điện Máy Quang Hà PT
UPDATE route_stops SET sequence_order = 6 WHERE route_id = 7 AND store_id = 15; -- KH0015 - Điện Máy Hiền Hằng PT
UPDATE route_stops SET sequence_order = 7 WHERE route_id = 7 AND store_id = 16; -- KH0016 - Điện Máy Anh Tuấn PT
UPDATE route_stops SET sequence_order = 8 WHERE route_id = 7 AND store_id = 18; -- KH0018 - Cửa Hàng Lan Thành PT
UPDATE route_stops SET sequence_order = 9 WHERE route_id = 7 AND store_id = 17; -- KH0017 - Điện Máy Thanh Vinh PT
UPDATE route_stops SET sequence_order = 10 WHERE route_id = 7 AND store_id = 14; -- KH0014 - Điện Máy Trường Giang PT 1

-- Sequence for Route ID 8 (13 stops)
UPDATE route_stops SET sequence_order = 1 WHERE route_id = 8 AND store_id = 70; -- KH0070 - Điện Máy Ngọc Hiển TN
UPDATE route_stops SET sequence_order = 2 WHERE route_id = 8 AND store_id = 73; -- KH0073 - Điện Máy Dũng Lam TN
UPDATE route_stops SET sequence_order = 3 WHERE route_id = 8 AND store_id = 77; -- KH0077 - Tổng Kho Điện Máy Gia Hân TN
UPDATE route_stops SET sequence_order = 4 WHERE route_id = 8 AND store_id = 80; -- KH0081 - Điện Máy  Xuân Thọ TN
UPDATE route_stops SET sequence_order = 5 WHERE route_id = 8 AND store_id = 71; -- KH0071 - Điện Máy Trung Xuân TN
UPDATE route_stops SET sequence_order = 6 WHERE route_id = 8 AND store_id = 75; -- KH0075 - Điện Máy Hòa Dương TN
UPDATE route_stops SET sequence_order = 7 WHERE route_id = 8 AND store_id = 72; -- KH0072 - Điện Máy Gia Huy TN
UPDATE route_stops SET sequence_order = 8 WHERE route_id = 8 AND store_id = 78; -- KH0079 - Cửa hàng Hưng Hưởng TN
UPDATE route_stops SET sequence_order = 9 WHERE route_id = 8 AND store_id = 64; -- KH0064 - Điện máy Anh Quảng TQ
UPDATE route_stops SET sequence_order = 10 WHERE route_id = 8 AND store_id = 9; -- KH0009 - Điện Máy Huyền Linh TQ
UPDATE route_stops SET sequence_order = 11 WHERE route_id = 8 AND store_id = 63; -- KH0063 - Điện Máy Thành Tuyên TQ
UPDATE route_stops SET sequence_order = 12 WHERE route_id = 8 AND store_id = 69; -- KH0069 - Điện Máy Bảo Anh TQ
UPDATE route_stops SET sequence_order = 13 WHERE route_id = 8 AND store_id = 66; -- KH0066 - Điện Máy Minh Tuyên TQ

-- Sequence for Route ID 9 (7 stops)
UPDATE route_stops SET sequence_order = 1 WHERE route_id = 9 AND store_id = 52; -- KH0052 - Điện Máy Thu Hiền LS
UPDATE route_stops SET sequence_order = 2 WHERE route_id = 9 AND store_id = 49; -- KH0049 - Điện Máy Thanh Tâm LS
UPDATE route_stops SET sequence_order = 3 WHERE route_id = 9 AND store_id = 74; -- KH0074 - Điện Máy Trâm Hằng TN
UPDATE route_stops SET sequence_order = 4 WHERE route_id = 9 AND store_id = 47; -- KH0047 - Điện Máy Hoa Thọ LS
UPDATE route_stops SET sequence_order = 5 WHERE route_id = 9 AND store_id = 48; -- KH0048 - Điện Máy Trung Kiên LS
UPDATE route_stops SET sequence_order = 6 WHERE route_id = 9 AND store_id = 51; -- KH0051 - Điện Máy Sơn Thùy LS
UPDATE route_stops SET sequence_order = 7 WHERE route_id = 9 AND store_id = 76; -- KH0076 - SC  Tùng Thành

-- Sequence for Route ID 10 (7 stops)
UPDATE route_stops SET sequence_order = 1 WHERE route_id = 10 AND store_id = 62; -- KH0062 - Điện Máy Luyến Na BK
UPDATE route_stops SET sequence_order = 2 WHERE route_id = 10 AND store_id = 59; -- KH0059 - Điện Máy Thủy Tuế BK
UPDATE route_stops SET sequence_order = 3 WHERE route_id = 10 AND store_id = 53; -- KH0053 - Điện Máy Nam Thủy BK
UPDATE route_stops SET sequence_order = 4 WHERE route_id = 10 AND store_id = 81; -- KH0083 - Điện Máy Tuyết Khải BK
UPDATE route_stops SET sequence_order = 5 WHERE route_id = 10 AND store_id = 56; -- KH0056 - Điện Máy Minh Chiến BK
UPDATE route_stops SET sequence_order = 6 WHERE route_id = 10 AND store_id = 82; -- KH0084 - Điện Máy Hoa Lợi BK
UPDATE route_stops SET sequence_order = 7 WHERE route_id = 10 AND store_id = 84; -- KH0086 - Điện Máy Thanh Điệp BK ( Thành Điệp BC )

-- Sequence for Route ID 11 (11 stops)
UPDATE route_stops SET sequence_order = 1 WHERE route_id = 11 AND store_id = 121; -- KH0123 - Điện Máy Tấn Tài HD
UPDATE route_stops SET sequence_order = 2 WHERE route_id = 11 AND store_id = 119; -- KH0121 - Điện Máy Nhật Bản HD
UPDATE route_stops SET sequence_order = 3 WHERE route_id = 11 AND store_id = 113; -- KH0115 - Điện Máy Sơn Hà HP
UPDATE route_stops SET sequence_order = 4 WHERE route_id = 11 AND store_id = 123; -- KH0125 - Điện Máy Tấn Tài HP
UPDATE route_stops SET sequence_order = 5 WHERE route_id = 11 AND store_id = 115; -- KH0117 - Điện máy Tuyên Loan
UPDATE route_stops SET sequence_order = 6 WHERE route_id = 11 AND store_id = 114; -- KH0116 - Điện Máy Quảng Thi HP
UPDATE route_stops SET sequence_order = 7 WHERE route_id = 11 AND store_id = 122; -- KH0124 - Điện Tử - Điện Lạnh Nhật Minh HP
UPDATE route_stops SET sequence_order = 8 WHERE route_id = 11 AND store_id = 118; -- KH0120 - Điện Máy Hoàng Khoát HP
UPDATE route_stops SET sequence_order = 9 WHERE route_id = 11 AND store_id = 112; -- KH0114 - Điện máy Trường Hiền HP
UPDATE route_stops SET sequence_order = 10 WHERE route_id = 11 AND store_id = 120; -- KH0122 - Trung Tâm Điện Máy Hưng Thúy HD
UPDATE route_stops SET sequence_order = 11 WHERE route_id = 11 AND store_id = 116; -- KH0118 - Điện Máy Thủy Sản GL

-- Sequence for Route ID 12 (8 stops)
UPDATE route_stops SET sequence_order = 1 WHERE route_id = 12 AND store_id = 149; -- KH0151 - Điện Máy Thanh Bình BT
UPDATE route_stops SET sequence_order = 2 WHERE route_id = 12 AND store_id = 148; -- KH0150 - Điện Máy  Đức Huỳnh BT
UPDATE route_stops SET sequence_order = 3 WHERE route_id = 12 AND store_id = 144; -- KH0146 - Điện Máy  Thành Đạt BT
UPDATE route_stops SET sequence_order = 4 WHERE route_id = 12 AND store_id = 143; -- KH0145 - Điện Máy Kiều Trụ BT
UPDATE route_stops SET sequence_order = 5 WHERE route_id = 12 AND store_id = 141; -- KH0143 - Điện Máy Nghĩa Huệ BT
UPDATE route_stops SET sequence_order = 6 WHERE route_id = 12 AND store_id = 146; -- KH0148 - Điện Máy Huy Khánh BT 2
UPDATE route_stops SET sequence_order = 7 WHERE route_id = 12 AND store_id = 140; -- KH0142 - Điện máy Tuấn Sơn CS2 BT
UPDATE route_stops SET sequence_order = 8 WHERE route_id = 12 AND store_id = 24; -- KH0024 - Điện Máy Trường Thủy SS

-- Clear route cache (polylines/distances) so system recalculates updated routes
UPDATE routes SET route_polyline = NULL, total_distance_km = NULL, total_duration_min = NULL WHERE id IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

SET FOREIGN_KEY_CHECKS = 1;