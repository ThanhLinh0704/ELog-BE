-- ============================================================
-- ELog Delivery Management System
-- Flyway Migration: V27__fix_stores_ward_codes.sql
-- Correct ward and district codes for 13 stores in Northern Region
-- ============================================================

-- ST-007: Điện Máy Xanh Phùng (Đan Phượng, Hà Nội) - correct ward is '09784' (Thị trấn Phùng)
UPDATE stores SET ward_code = '09784' WHERE id = 7;

-- ST-020: Điện Máy Xanh Cổ Bi (Gia Lâm, Hà Nội) - correct ward is '00553' (Xã Cổ Bi)
UPDATE stores SET ward_code = '00553' WHERE id = 20;

-- ST-023: MediaMart Yên Phong (Yên Phong, Bắc Ninh) - correct district is '258' (Yên Phong), ward is '09193' (Thị trấn Chờ)
UPDATE stores SET district_code = '258', ward_code = '09193' WHERE id = 23;

-- ST-024: Điện Máy Xanh Yên Phong (Yên Phong, Bắc Ninh) - correct district is '258' (Yên Phong), ward is '09205' (Xã Yên Trung)
UPDATE stores SET district_code = '258', ward_code = '09205' WHERE id = 24;

-- ST-025: Nguyễn Kim Bắc Ninh (Thành phố Bắc Ninh) - correct ward is '09163' (Phường Vũ Ninh)
UPDATE stores SET ward_code = '09163' WHERE id = 25;

-- ST-036: MediaMart Như Quỳnh (Văn Lâm, Hưng Yên) - correct ward is '11986' (Thị trấn Như Quỳnh)
UPDATE stores SET ward_code = '11986' WHERE id = 36;

-- ST-042: Điện Máy Xanh Hải Dương (Thành phố Hải Dương) - correct ward is '10525' (Phường Trần Hưng Đạo)
UPDATE stores SET ward_code = '10525' WHERE id = 42;

-- ST-043: MediaMart Quang Trung (Thành phố Hải Dương) - correct ward is '10516' (Phường Quang Trung)
UPDATE stores SET ward_code = '10516' WHERE id = 43;

-- ST-044: Nguyễn Kim Kim Thành (Kim Thành, Hải Dương) - correct district is '293' (Kim Thành), ward is '10750' (Thị trấn Phú Thái)
UPDATE stores SET district_code = '293', ward_code = '10750' WHERE id = 44;

-- ST-045: Điện Máy Chợ Lớn Kim Thành (Kim Thành, Hải Dương) - correct district is '293' (Kim Thành), ward is '10804' (Xã Đồng Cẩm)
UPDATE stores SET district_code = '293', ward_code = '10804' WHERE id = 45;

-- ST-048: Nguyễn Kim Hồng Bàng (Hồng Bàng, Hải Phòng) - correct ward is '11311' (Phường Minh Khai)
UPDATE stores SET ward_code = '11311' WHERE id = 48;

-- ST-049: Điện Máy Xanh Minh Khai (Hồng Bàng, Hải Phòng) - correct ward is '11311' (Phường Minh Khai)
UPDATE stores SET ward_code = '11311' WHERE id = 49;

-- ST-050: MediaMart Hải Phòng (Hồng Bàng, Hải Phòng) - correct ward is '11299' (Phường Hùng Vương)
UPDATE stores SET ward_code = '11299' WHERE id = 50;
