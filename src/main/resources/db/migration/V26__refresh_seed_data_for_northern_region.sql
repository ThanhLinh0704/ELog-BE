-- V26__refresh_seed_data_for_northern_region.sql
SET FOREIGN_KEY_CHECKS = 0;

-- 1. Clean existing transactional and reference data
TRUNCATE TABLE trip_stops;
TRUNCATE TABLE trips;
TRUNCATE TABLE manifests;
TRUNCATE TABLE trip_draft_stops;
TRUNCATE TABLE trip_drafts;
TRUNCATE TABLE route_stops;
TRUNCATE TABLE routes;
TRUNCATE TABLE order_items;
TRUNCATE TABLE orders;
TRUNCATE TABLE import_errors;
TRUNCATE TABLE import_batches;
TRUNCATE TABLE stores;
TRUNCATE TABLE vehicles;
TRUNCATE TABLE products;

-- 2. Update warehouse coordinates to Hanoi (ĐT70, Xuân Phương, Nam Từ Liêm, Hà Nội)
UPDATE system_config SET config_value = '21.0315' WHERE config_key = 'WAREHOUSE_LAT';
UPDATE system_config SET config_value = '105.7491' WHERE config_key = 'WAREHOUSE_LNG';

-- 3. Reseed 24 realistic electrical appliances (Products)
INSERT INTO products (id, sku, product_name, weight_kg, length_m, width_m, height_m, volume_m3, is_active, shape, is_fragile, package_image_url, description) VALUES
(1, 'TV-LG-32', 'Smart TV LG 32 inch', 6.000, 0.8000, 0.1500, 0.5000, 0.060000, TRUE, 'FLAT_BOX', TRUE, '/images/products/tv_lg_32.png', 'Smart TV LG 32 inch HD ready'),
(2, 'TV-SAM-43', 'Smart TV Samsung 43 inch', 9.500, 1.0500, 0.1500, 0.6500, 0.102375, TRUE, 'FLAT_BOX', TRUE, '/images/products/tv_sam_43.png', 'Smart TV Samsung 43 inch Crystal UHD 4K'),
(3, 'TV-SONY-55', 'Smart TV Sony 55 inch', 17.000, 1.3500, 0.1800, 0.8500, 0.206550, TRUE, 'FLAT_BOX', TRUE, '/images/products/tv_sony_55.png', 'Smart TV Sony 55 inch 4K Ultra HD'),
(4, 'TV-SAM-65', 'Smart TV Samsung 65 inch', 24.000, 1.6000, 0.2000, 0.9500, 0.304000, TRUE, 'FLAT_BOX', TRUE, '/images/products/tv_sam_65.png', 'Smart TV Samsung 65 inch Neo QLED 4K'),
(5, 'TV-LG-75', 'Smart TV LG 75 inch', 35.000, 1.8500, 0.2200, 1.1000, 0.447700, TRUE, 'FLAT_BOX', TRUE, '/images/products/tv_lg_75.png', 'Smart TV LG 75 inch NanoCell 4K'),
(6, 'REF-TOS-180', 'Tủ lạnh Toshiba 180L', 42.000, 0.5500, 0.6000, 1.4000, 0.462000, TRUE, 'UPRIGHT_BOX', FALSE, '/images/products/ref_tos_180.png', 'Tủ lạnh Toshiba 180L Inverter'),
(7, 'REF-SAM-250', 'Tủ lạnh Samsung Inverter 250L', 53.000, 0.6000, 0.6500, 1.6000, 0.624000, TRUE, 'UPRIGHT_BOX', FALSE, '/images/products/ref_sam_250.png', 'Tủ lạnh Samsung 250L hai cánh'),
(8, 'REF-LG-350', 'Tủ lạnh LG Side by Side 350L', 78.000, 0.7000, 0.7000, 1.7500, 0.857500, TRUE, 'UPRIGHT_BOX', FALSE, '/images/products/ref_lg_350.png', 'Tủ lạnh LG Side by Side 350L cao cấp'),
(9, 'REF-HIT-450', 'Tủ lạnh Hitachi cao cấp 450L', 95.000, 0.8000, 0.7500, 1.8500, 1.110000, TRUE, 'UPRIGHT_BOX', FALSE, '/images/products/ref_hit_450.png', 'Tủ lạnh Hitachi 450L đa cửa nhập khẩu'),
(10, 'WM-ELX-7', 'Máy giặt Electrolux cửa trước 7kg', 62.000, 0.6000, 0.6000, 0.8500, 0.306000, TRUE, 'RECTANGULAR_BOX', FALSE, '/images/products/wm_elx_7.png', 'Máy giặt Electrolux 7kg lồng ngang'),
(11, 'WM-LG-8', 'Máy giặt LG Lồng ngang 8kg', 65.000, 0.6000, 0.6200, 0.8500, 0.316200, TRUE, 'RECTANGULAR_BOX', FALSE, '/images/products/wm_lg_8.png', 'Máy giặt LG AI DD 8kg'),
(12, 'WM-SAM-10', 'Máy giặt Samsung 10kg', 72.000, 0.6500, 0.6500, 0.9000, 0.380250, TRUE, 'RECTANGULAR_BOX', FALSE, '/images/products/wm_sam_10.png', 'Máy giặt thông minh Samsung EcoBubble 10kg'),
(13, 'WM-TOS-12', 'Máy giặt Toshiba lồng đứng 12kg', 50.000, 0.6500, 0.6800, 1.0000, 0.442000, TRUE, 'RECTANGULAR_BOX', FALSE, '/images/products/wm_tos_12.png', 'Máy giặt Toshiba 12kg cửa trên'),
(14, 'AC-PAN-1', 'Điều hòa Panasonic 1 HP', 32.000, 0.9000, 0.3500, 0.6000, 0.189000, TRUE, 'CUBOID', TRUE, '/images/products/ac_pan_1.png', 'Điều hòa Panasonic Inverter 1 HP cục lạnh + nóng'),
(15, 'AC-DAI-15', 'Điều hòa Daikin 1.5 HP', 38.000, 0.9500, 0.3800, 0.6500, 0.234650, TRUE, 'CUBOID', TRUE, '/images/products/ac_dai_15.png', 'Điều hòa Daikin Inverter 1.5 HP cục lạnh + nóng'),
(16, 'AC-SAM-2', 'Điều hòa Samsung 2 HP', 46.000, 1.0500, 0.4000, 0.7500, 0.315000, TRUE, 'CUBOID', TRUE, '/images/products/ac_sam_2.png', 'Điều hòa Samsung Wind-Free 2 HP cục lạnh + nóng'),
(17, 'DRY-ELX-8', 'Máy sấy quần áo Electrolux 8kg', 40.000, 0.6000, 0.6000, 0.8500, 0.306000, TRUE, 'RECTANGULAR_BOX', FALSE, '/images/products/dry_elx_8.png', 'Máy sấy thông hơi Electrolux 8kg'),
(18, 'DW-BOSCH-12', 'Máy rửa bát Bosch 12 bộ', 48.000, 0.6000, 0.6000, 0.8500, 0.306000, TRUE, 'RECTANGULAR_BOX', TRUE, '/images/products/dw_bosch_12.png', 'Máy rửa bát Bosch độc lập 12 bộ chén bát'),
(19, 'MW-SHARP-20', 'Lò vi sóng Sharp 20L', 12.000, 0.4500, 0.3500, 0.3000, 0.047250, TRUE, 'RECTANGULAR_BOX', TRUE, '/images/products/mw_sharp_20.png', 'Lò vi sóng Sharp 20L nút vặn cơ'),
(20, 'OV-TEF-30', 'Lò nướng Tefal 30L', 15.000, 0.5000, 0.4000, 0.3500, 0.070000, TRUE, 'RECTANGULAR_BOX', TRUE, '/images/products/ov_tef_30.png', 'Lò nướng điện Tefal 30L đối lưu'),
(21, 'AF-PHIL-5', 'Nồi chiên không dầu Philips 5L', 7.000, 0.3500, 0.3500, 0.4000, 0.049000, TRUE, 'CUBOID', FALSE, '/images/products/af_phil_5.png', 'Nồi chiên không dầu Philips Rapid Air 5L'),
(22, 'VC-DYSON-10', 'Máy hút bụi cầm tay Dyson', 4.500, 0.3000, 0.3000, 1.2000, 0.108000, TRUE, 'FLAT_BOX', FALSE, '/images/products/vc_dyson_10.png', 'Máy hút bụi không dây Dyson V10'),
(23, 'EF-SENKO', 'Quạt đứng Senko', 6.000, 0.4500, 0.4500, 1.2000, 0.243000, TRUE, 'CUBOID', FALSE, '/images/products/ef_senko.png', 'Quạt lửng Senko chất lượng cao'),
(24, 'WH-ARIS-20', 'Bình nóng lạnh Ariston 20L', 12.000, 0.4500, 0.4000, 0.4000, 0.072000, TRUE, 'RECTANGULAR_BOX', FALSE, '/images/products/wh_aris_20.png', 'Bình nóng lạnh gián tiếp Ariston 20L');

-- 4. Reseed 30 vehicles
INSERT INTO vehicles (id, vehicle_code, plate_number, vehicle_type, vehicle_class, payload_kg, gross_vehicle_weight_kg, required_license, max_volume_m3, cargo_length_mm, cargo_width_mm, cargo_height_mm, average_speed_kmh, cost_per_km, status, is_active, image_url, permit_info, description) VALUES
(1,'XE001','29H-12001','TRUCK','1.25T',1250.00,3490.00,'B',8.000,3100,1700,1500,38.00,12000.00,'AVAILABLE',TRUE,'/images/vehicles/truck_1250.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 1.25T phục vụ vận chuyển hàng điện máy.'),
(2,'XE002','29H-12002','TRUCK','1.25T',1250.00,3490.00,'B',8.000,3100,1700,1500,38.00,12000.00,'AVAILABLE',TRUE,'/images/vehicles/truck_1250.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 1.25T phục vụ vận chuyển hàng điện máy.'),
(3,'XE003','29H-12003','TRUCK','1.25T',1250.00,3490.00,'B',8.000,3100,1700,1500,38.00,12000.00,'AVAILABLE',TRUE,'/images/vehicles/truck_1250.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 1.25T phục vụ vận chuyển hàng điện máy.'),
(4,'XE004','29H-12004','TRUCK','1.25T',1250.00,3490.00,'B',8.000,3100,1700,1500,38.00,12000.00,'AVAILABLE',TRUE,'/images/vehicles/truck_1250.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 1.25T phục vụ vận chuyển hàng điện máy.'),
(5,'XE005','29H-12005','TRUCK','1.25T',1250.00,3490.00,'B',8.000,3100,1700,1500,38.00,12000.00,'AVAILABLE',TRUE,'/images/vehicles/truck_1250.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 1.25T phục vụ vận chuyển hàng điện máy.'),
(6,'XE006','29H-12006','TRUCK','1.25T',1250.00,3490.00,'B',8.000,3100,1700,1500,38.00,12000.00,'AVAILABLE',TRUE,'/images/vehicles/truck_1250.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 1.25T phục vụ vận chuyển hàng điện máy.'),
(7,'XE007','29H-12007','TRUCK','1.25T',1250.00,3490.00,'B',8.000,3100,1700,1500,38.00,12000.00,'AVAILABLE',TRUE,'/images/vehicles/truck_1250.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 1.25T phục vụ vận chuyển hàng điện máy.'),
(8,'XE008','29H-12008','TRUCK','1.25T',1250.00,3490.00,'B',8.000,3100,1700,1500,38.00,12000.00,'AVAILABLE',TRUE,'/images/vehicles/truck_1250.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 1.25T phục vụ vận chuyển hàng điện máy.'),
(9,'XE009','29H-12009','TRUCK','1.25T',1250.00,3490.00,'B',8.000,3100,1700,1500,38.00,12000.00,'AVAILABLE',TRUE,'/images/vehicles/truck_1250.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 1.25T phục vụ vận chuyển hàng điện máy.'),
(10,'XE010','29H-12010','TRUCK','1.25T',1250.00,3490.00,'B',8.000,3100,1700,1500,38.00,12000.00,'AVAILABLE',TRUE,'/images/vehicles/truck_1250.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 1.25T phục vụ vận chuyển hàng điện máy.'),
(11,'XE011','29H-12011','TRUCK','2.5T',2500.00,5200.00,'C1',15.000,4300,1800,1900,40.00,14500.00,'AVAILABLE',TRUE,'/images/vehicles/truck_2500.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 2.5T phục vụ vận chuyển hàng điện máy.'),
(12,'XE012','29H-12012','TRUCK','2.5T',2500.00,5200.00,'C1',15.000,4300,1800,1900,40.00,14500.00,'AVAILABLE',TRUE,'/images/vehicles/truck_2500.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 2.5T phục vụ vận chuyển hàng điện máy.'),
(13,'XE013','29H-12013','TRUCK','2.5T',2500.00,5200.00,'C1',15.000,4300,1800,1900,40.00,14500.00,'AVAILABLE',TRUE,'/images/vehicles/truck_2500.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 2.5T phục vụ vận chuyển hàng điện máy.'),
(14,'XE014','29H-12014','TRUCK','2.5T',2500.00,5200.00,'C1',15.000,4300,1800,1900,40.00,14500.00,'AVAILABLE',TRUE,'/images/vehicles/truck_2500.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 2.5T phục vụ vận chuyển hàng điện máy.'),
(15,'XE015','29H-12015','TRUCK','2.5T',2500.00,5200.00,'C1',15.000,4300,1800,1900,40.00,14500.00,'AVAILABLE',TRUE,'/images/vehicles/truck_2500.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 2.5T phục vụ vận chuyển hàng điện máy.'),
(16,'XE016','29H-12016','TRUCK','2.5T',2500.00,5200.00,'C1',15.000,4300,1800,1900,40.00,14500.00,'AVAILABLE',TRUE,'/images/vehicles/truck_2500.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 2.5T phục vụ vận chuyển hàng điện máy.'),
(17,'XE017','29H-12017','TRUCK','2.5T',2500.00,5200.00,'C1',15.000,4300,1800,1900,40.00,14500.00,'AVAILABLE',TRUE,'/images/vehicles/truck_2500.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 2.5T phục vụ vận chuyển hàng điện máy.'),
(18,'XE018','29H-12018','TRUCK','2.5T',2500.00,5200.00,'C1',15.000,4300,1800,1900,40.00,14500.00,'AVAILABLE',TRUE,'/images/vehicles/truck_2500.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 2.5T phục vụ vận chuyển hàng điện máy.'),
(19,'XE019','29H-12019','TRUCK','3.5T',3500.00,7000.00,'C1',20.000,5200,1900,2100,42.00,16500.00,'AVAILABLE',TRUE,'/images/vehicles/truck_3500.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 3.5T phục vụ vận chuyển hàng điện máy.'),
(20,'XE020','29H-12020','TRUCK','3.5T',3500.00,7000.00,'C1',20.000,5200,1900,2100,42.00,16500.00,'AVAILABLE',TRUE,'/images/vehicles/truck_3500.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 3.5T phục vụ vận chuyển hàng điện máy.'),
(21,'XE021','29H-12021','TRUCK','3.5T',3500.00,7000.00,'C1',20.000,5200,1900,2100,42.00,16500.00,'AVAILABLE',TRUE,'/images/vehicles/truck_3500.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 3.5T phục vụ vận chuyển hàng điện máy.'),
(22,'XE022','29H-12022','TRUCK','3.5T',3500.00,7000.00,'C1',20.000,5200,1900,2100,42.00,16500.00,'AVAILABLE',TRUE,'/images/vehicles/truck_3500.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 3.5T phục vụ vận chuyển hàng điện máy.'),
(23,'XE023','29H-12023','TRUCK','3.5T',3500.00,7000.00,'C1',20.000,5200,1900,2100,42.00,16500.00,'AVAILABLE',TRUE,'/images/vehicles/truck_3500.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 3.5T phục vụ vận chuyển hàng điện máy.'),
(24,'XE024','29H-12024','TRUCK','3.5T',3500.00,7000.00,'C1',20.000,5200,1900,2100,42.00,16500.00,'AVAILABLE',TRUE,'/images/vehicles/truck_3500.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 3.5T phục vụ vận chuyển hàng điện máy.'),
(25,'XE025','29H-12025','TRUCK','5T',5000.00,9000.00,'C',30.000,6200,2200,2200,45.00,19000.00,'AVAILABLE',TRUE,'/images/vehicles/truck_5000.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 5T phục vụ vận chuyển hàng điện máy.'),
(26,'XE026','29H-12026','TRUCK','5T',5000.00,9000.00,'C',30.000,6200,2200,2200,45.00,19000.00,'AVAILABLE',TRUE,'/images/vehicles/truck_5000.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 5T phục vụ vận chuyển hàng điện máy.'),
(27,'XE027','29H-12027','TRUCK','5T',5000.00,9000.00,'C',30.000,6200,2200,2200,45.00,19000.00,'AVAILABLE',TRUE,'/images/vehicles/truck_5000.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 5T phục vụ vận chuyển hàng điện máy.'),
(28,'XE028','29H-12028','TRUCK','5T',5000.00,9000.00,'C',30.000,6200,2200,2200,45.00,19000.00,'AVAILABLE',TRUE,'/images/vehicles/truck_5000.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 5T phục vụ vận chuyển hàng điện máy.'),
(29,'XE029','29H-12029','TRUCK','8T',8000.00,14500.00,'C',45.000,7600,2300,2500,48.00,24000.00,'AVAILABLE',TRUE,'/images/vehicles/truck_8000.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 8T phục vụ vận chuyển hàng điện máy.'),
(30,'XE030','29H-12030','TRUCK','8T',8000.00,14500.00,'C',45.000,7600,2300,2500,48.00,24000.00,'AVAILABLE',TRUE,'/images/vehicles/truck_8000.png','{"inspectionExpiry":"2027-03-15","insuranceExpiry":"2027-01-18","roadFeeExpiry":"2026-12-31"}','Xe tải 8T phục vụ vận chuyển hàng điện máy.');

-- 5. Reseed drivers with varied license classes
UPDATE users SET license_class = 'B' WHERE id = 4;
DELETE FROM user_roles WHERE user_id >= 6;
DELETE FROM users WHERE id >= 6;

INSERT INTO users (id, username, email, password_hash, full_name, is_active, license_class) VALUES
(6, 'driver02', 'driver02@elog.vn', '$2a$12$dfT1VXhfjzIm5GRAjwgYg.5O8XQ.mCMmneaFuXSOLrpXGRRNApJxG', 'Nguyễn Văn Bính', TRUE, 'C1'),
(7, 'driver03', 'driver03@elog.vn', '$2a$12$dfT1VXhfjzIm5GRAjwgYg.5O8XQ.mCMmneaFuXSOLrpXGRRNApJxG', 'Trần Văn Cường', TRUE, 'C'),
(8, 'driver04', 'driver04@elog.vn', '$2a$12$dfT1VXhfjzIm5GRAjwgYg.5O8XQ.mCMmneaFuXSOLrpXGRRNApJxG', 'Lê Hoàng Đức', TRUE, 'B'),
(9, 'driver05', 'driver05@elog.vn', '$2a$12$dfT1VXhfjzIm5GRAjwgYg.5O8XQ.mCMmneaFuXSOLrpXGRRNApJxG', 'Phạm Tiến Đạt', TRUE, 'C1'),
(10, 'driver06', 'driver06@elog.vn', '$2a$12$dfT1VXhfjzIm5GRAjwgYg.5O8XQ.mCMmneaFuXSOLrpXGRRNApJxG', 'Vũ Quốc Việt', TRUE, 'C');

INSERT INTO user_roles (user_id, role_id) VALUES
(6, 4),
(7, 4),
(8, 4),
(9, 4),
(10, 4);

-- 6. Reseed 50 Stores in Northern Region
INSERT INTO stores (id, code, name, province_code, district_code, ward_code, address_detail, latitude, longitude, allowed_delivery_hours, max_allowed_vehicle_weight, image_url, is_active) VALUES
-- Tuyến 1 (Tây Bắc) - Stores 1 to 16
(1, 'ST-001', 'Điện Máy Xanh Cầu Diễn', '01', '019', '00613', 'Số 12 Đường Cầu Diễn', 21.031000, 105.753000, '08:00-17:00', 8000.000, '/images/stores/store_001.png', TRUE),
(2, 'ST-002', 'Nguyễn Kim Mỹ Đình', '01', '019', '00616', 'Số 45 Lê Đức Thọ, Mỹ Đình 1', 21.032000, 105.758000, '08:00-17:00', 8000.000, '/images/stores/store_002.png', TRUE),
(3, 'ST-003', 'MediaMart Mỹ Đình 2', '01', '019', '00619', 'Số 78 Nguyễn Hoàng, Mỹ Đình 2', 21.035000, 105.762000, '08:00-17:00', 8000.000, '/images/stores/store_003.png', TRUE),
(4, 'ST-004', 'Điện Máy Xanh Vân Canh', '01', '274', '09787', 'Ngã tư Vân Canh, Hoài Đức', 21.042000, 105.742000, '08:00-17:00', 8000.000, '/images/stores/store_004.png', TRUE),
(5, 'ST-005', 'Chợ Sắt Trạm Trôi', '01', '274', '09778', 'Khu đô thị Lideco, Trạm Trôi', 21.047000, 105.735000, '08:00-17:00', 8000.000, '/images/stores/store_005.png', TRUE),
(6, 'ST-006', 'MediaMart Kim Chung', '01', '274', '09793', 'Số 150 Quốc lộ 32, Kim Chung', 21.052000, 105.728000, '08:00-17:00', 8000.000, '/images/stores/store_006.png', TRUE),
(7, 'ST-007', 'Điện Máy Xanh Phùng', '01', '273', '09736', 'Số 200 Tây Sơn, Thị trấn Phùng', 21.065000, 105.715000, '08:00-17:00', 5000.000, '/images/stores/store_007.png', TRUE),
(8, 'ST-008', 'Nguyễn Kim Đan Phượng', '01', '273', '09742', 'Cụm công nghiệp Đan Phượng', 21.075000, 105.702000, '08:00-17:00', 5000.000, '/images/stores/store_008.png', TRUE),
(9, 'ST-009', 'Điện Máy Chợ Lớn Phúc Thọ', '01', '272', '09700', 'Số 50 Lạc Trị, Phúc Thọ', 21.085000, 105.680000, '08:00-17:00', 5000.000, '/images/stores/store_009.png', TRUE),
(10, 'ST-010', 'MediaMart Thọ Lộc', '01', '272', '09703', 'Ngã tư Thọ Lộc, Phúc Thọ', 21.095000, 105.650000, '08:00-17:00', 5000.000, '/images/stores/store_010.png', TRUE),
(11, 'ST-011', 'Điện Máy Xanh Trưng Trắc', '26', '244', '08713', 'Số 15 Trưng Trắc, Phúc Yên', 21.185000, 105.720000, '08:00-17:00', 8000.000, '/images/stores/store_011.png', TRUE),
(12, 'ST-012', 'Nguyễn Kim Phúc Yên', '26', '244', '08716', 'Số 100 Trần Hưng Đạo, Phúc Yên', 21.210000, 105.710000, '08:00-17:00', 8000.000, '/images/stores/store_012.png', TRUE),
(13, 'ST-013', 'Điện Máy Xanh Liên Bảo', '26', '243', '08671', 'Số 250 Hùng Vương, Vĩnh Yên', 21.300000, 105.600000, '08:00-17:00', 15000.000, '/images/stores/store_013.png', TRUE),
(14, 'ST-014', 'MediaMart Tích Sơn', '26', '243', '08674', 'Số 80 Phạm Văn Đồng, Vĩnh Yên', 21.310000, 105.580000, '08:00-17:00', 15000.000, '/images/stores/store_014.png', TRUE),
(15, 'ST-015', 'Nguyễn Kim Việt Trì', '25', '230', '08164', 'Số 1800 Hùng Vương, Việt Trì', 21.320000, 105.420000, '08:00-17:00', 15000.000, '/images/stores/store_015.png', TRUE),
(16, 'ST-016', 'Điện Máy Xanh Thanh Miếu', '25', '230', '08170', 'Số 10 Tiên Sơn, Việt Trì', 21.330000, 105.400000, '08:00-17:00', 15000.000, '/images/stores/store_016.png', TRUE),

-- Tuyến 2 (Đông Bắc) - Stores 17 to 33
(17, 'ST-017', 'Điện Máy Xanh Bồ Đề', '01', '004', '00127', 'Số 120 Lâm Du, Bồ Đề', 21.045000, 105.880000, '08:00-17:00', 8000.000, '/images/stores/store_017.png', TRUE),
(18, 'ST-018', 'Nguyễn Kim Gia Thụy', '01', '004', '00130', 'Số 450 Nguyễn Văn Cừ, Gia Thụy', 21.055000, 105.890000, '08:00-17:00', 8000.000, '/images/stores/store_018.png', TRUE),
(19, 'ST-019', 'MediaMart Yên Viên', '01', '018', '00583', 'Số 50 Hà Huy Tập, Yên Viên', 21.070000, 105.920000, '08:00-17:00', 8000.000, '/images/stores/store_019.png', TRUE),
(20, 'ST-020', 'Điện Máy Xanh Cổ Bi', '01', '018', '00586', 'Số 20 Cổ Bi, Gia Lâm', 21.085000, 105.940000, '08:00-17:00', 8000.000, '/images/stores/store_020.png', TRUE),
(21, 'ST-021', 'Điện Máy Chợ Lớn Từ Sơn', '27', '258', '09277', 'Số 10 Trần Phú, Từ Sơn', 21.110000, 105.960000, '08:00-17:00', 8000.000, '/images/stores/store_021.png', TRUE),
(22, 'ST-022', 'Nguyễn Kim Đồng Nguyên', '27', '258', '09280', 'Số 85 Lý Thái Tổ, Đồng Nguyên', 21.125000, 105.975000, '08:00-17:00', 8000.000, '/images/stores/store_022.png', TRUE),
(23, 'ST-023', 'MediaMart Yên Phong', '27', '259', '09300', 'Số 12 Khu đô thị mới Chờ, Yên Phong', 21.180000, 105.980000, '08:00-17:00', 8000.000, '/images/stores/store_023.png', TRUE),
(24, 'ST-024', 'Điện Máy Xanh Yên Phong', '27', '259', '09300', 'Cụm công nghiệp Yên Phong', 21.200000, 105.970000, '08:00-17:00', 8000.000, '/images/stores/store_024.png', TRUE),
(25, 'ST-025', 'Nguyễn Kim Bắc Ninh', '27', '256', '09160', 'Số 35 Lý Thái Tổ, Vũ Ninh', 21.185000, 106.070000, '08:00-17:00', 15000.000, '/images/stores/store_025.png', TRUE),
(26, 'ST-026', 'Điện Máy Xanh Đáp Cầu', '27', '256', '09163', 'Số 150 Vũ Kiệt, Đáp Cầu', 21.195000, 106.080000, '08:00-17:00', 15000.000, '/images/stores/store_026.png', TRUE),
(27, 'ST-027', 'MediaMart Việt Yên', '24', '219', '07720', 'Khu phố Mới, Thị trấn Nếnh', 21.220000, 106.090000, '08:00-17:00', 15000.000, '/images/stores/store_027.png', TRUE),
(28, 'ST-028', 'Điện Máy Xanh Bích Động', '24', '219', '07723', 'Số 20 Quốc lộ 37, Bích Động', 21.230000, 106.100000, '08:00-17:00', 15000.000, '/images/stores/store_028.png', TRUE),
(29, 'ST-029', 'Nguyễn Kim Bắc Giang', '24', '213', '07537', 'Số 10 Hùng Vương, Trần Nguyên Hãn', 21.270000, 106.190000, '08:00-17:00', 15000.000, '/images/stores/store_029.png', TRUE),
(30, 'ST-030', 'MediaMart Bắc Giang', '24', '213', '07540', 'Số 180 Xương Giang, Lê Lợi', 21.280000, 106.200000, '08:00-17:00', 15000.000, '/images/stores/store_030.png', TRUE),
(31, 'ST-031', 'Điện Máy Xanh Vôi', '24', '213', '07540', 'Số 50 Hùng Vương, Lạng Giang', 21.350000, 106.250000, '08:00-17:00', 8000.000, '/images/stores/store_031.png', TRUE),
(32, 'ST-032', 'MediaMart Lạng Giang', '24', '213', '07540', 'Phố Vôi, Thị trấn Vôi, Lạng Giang', 21.380000, 106.280000, '08:00-17:00', 8000.000, '/images/stores/store_032.png', TRUE),
(33, 'ST-033', 'Điện Máy Chợ Lớn Lạng Giang', '24', '213', '07540', 'Số 120 Cần Trạm, Lạng Giang', 21.400000, 106.300000, '08:00-17:00', 8000.000, '/images/stores/store_033.png', TRUE),

-- Tuyến 3 (Phía Đông) - Stores 34 to 50
(34, 'ST-034', 'Điện Máy Xanh Văn Điển', '01', '020', '00643', 'Số 10 Ngọc Hồi, Văn Điển', 20.950000, 105.850000, '08:00-17:00', 8000.000, '/images/stores/store_034.png', TRUE),
(35, 'ST-035', 'Nguyễn Kim Tân Triều', '01', '020', '00646', 'Số 75 Phùng Hưng, Tân Triều', 20.940000, 105.860000, '08:00-17:00', 8000.000, '/images/stores/store_035.png', TRUE),
(36, 'ST-036', 'MediaMart Như Quỳnh', '33', '325', '11965', 'Số 20 Quốc lộ 5, Như Quỳnh', 20.980000, 106.020000, '08:00-17:00', 8000.000, '/images/stores/store_036.png', TRUE),
(37, 'ST-037', 'Điện Máy Xanh Lạc Đạo', '33', '325', '11968', 'Cụm công nghiệp Lạc Đạo', 20.970000, 106.040000, '08:00-17:00', 8000.000, '/images/stores/store_037.png', TRUE),
(38, 'ST-038', 'Nguyễn Kim Bần', '33', '327', '12025', 'Số 30 Hưng Yên, Bần Yên Nhân', 20.950000, 106.080000, '08:00-17:00', 8000.000, '/images/stores/store_038.png', TRUE),
(39, 'ST-039', 'MediaMart Mỹ Hào', '33', '327', '12028', 'Ngã tư Phố Nối, Mỹ Hào', 20.940000, 106.100000, '08:00-17:00', 8000.000, '/images/stores/store_039.png', TRUE),
(40, 'ST-040', 'Điện Máy Xanh Kẻ Sặt', '30', '294', '10567', 'Số 10 Hùng Vương, Kẻ Sặt', 20.930000, 106.180000, '08:00-17:00', 8000.000, '/images/stores/store_040.png', TRUE),
(41, 'ST-041', 'Nguyễn Kim Bình Giang', '30', '294', '10567', 'Số 180 Trần Hưng Đạo, Bình Giang', 20.920000, 106.200000, '08:00-17:00', 8000.000, '/images/stores/store_041.png', TRUE),
(42, 'ST-042', 'Điện Máy Xanh Hải Dương', '30', '288', '10306', 'Số 1 Hùng Vương, Trần Hưng Đạo', 20.940000, 106.330000, '08:00-17:00', 15000.000, '/images/stores/store_042.png', TRUE),
(43, 'ST-043', 'MediaMart Quang Trung', '30', '288', '10309', 'Số 150 Lê Thanh Nghị, Quang Trung', 20.930000, 106.350000, '08:00-17:00', 15000.000, '/images/stores/store_043.png', TRUE),
(44, 'ST-044', 'Nguyễn Kim Kim Thành', '30', '288', '10306', 'Số 200 Quốc lộ 5, Kim Thành', 20.950000, 106.480000, '08:00-17:00', 15000.000, '/images/stores/store_044.png', TRUE),
(45, 'ST-045', 'Điện Máy Chợ Lớn Kim Thành', '30', '288', '10306', 'Số 50 Đồng Gia, Kim Thành', 20.940000, 106.500000, '08:00-17:00', 15000.000, '/images/stores/store_045.png', TRUE),
(46, 'ST-046', 'Điện Máy Xanh An Dương', '31', '311', '11500', 'Số 10 Nguyễn Văn Linh, An Dương', 20.870000, 106.600000, '08:00-17:00', 15000.000, '/images/stores/store_046.png', TRUE),
(47, 'ST-047', 'MediaMart An Dương', '31', '311', '11500', 'Số 350 Lê Hồng Phong, An Dương', 20.860000, 106.620000, '08:00-17:00', 15000.000, '/images/stores/store_047.png', TRUE),
(48, 'ST-048', 'Nguyễn Kim Hồng Bàng', '31', '303', '11158', 'Số 25 Minh Khai, Hồng Bàng', 20.860000, 106.670000, '08:00-17:00', 15000.000, '/images/stores/store_048.png', TRUE),
(49, 'ST-049', 'Điện Máy Xanh Minh Khai', '31', '303', '11158', 'Số 80 Hoàng Văn Thụ, Hồng Bàng', 20.850000, 106.680000, '08:00-17:00', 15000.000, '/images/stores/store_049.png', TRUE),
(50, 'ST-050', 'MediaMart Hải Phòng', '31', '303', '11158', 'Số 5 Hùng Vương, Hải Phòng City', 20.840000, 106.700000, '08:00-17:00', 15000.000, '/images/stores/store_050.png', TRUE);

-- 7. Reseed 3 Routes
INSERT INTO routes (id, code, name, is_active) VALUES
(1, 'RT-HN-WEST', 'Tuyến Tây Bắc: HN - Vĩnh Phúc - Phú Thọ', TRUE),
(2, 'RT-HN-NE', 'Tuyến Đông Bắc: HN - Bắc Ninh - Bắc Giang', TRUE),
(3, 'RT-HN-EAST', 'Tuyến Phía Đông: HN - Hưng Yên - Hải Dương - Hải Phòng', TRUE);

-- 8. Reseed Route Stops
INSERT INTO route_stops (id, route_id, store_id, sequence_order, avg_service_time_min) VALUES
-- RT-HN-WEST Stops (Stores 1 to 16, sequenced from closest to furthest)
(1, 1, 1, 1, 15),
(2, 1, 2, 2, 15),
(3, 1, 3, 3, 15),
(4, 1, 4, 4, 15),
(5, 1, 5, 5, 15),
(6, 1, 6, 6, 15),
(7, 1, 7, 7, 15),
(8, 1, 8, 8, 15),
(9, 1, 9, 9, 15),
(10, 1, 10, 10, 15),
(11, 1, 11, 11, 20),
(12, 1, 12, 12, 20),
(13, 1, 13, 13, 20),
(14, 1, 14, 14, 20),
(15, 1, 15, 15, 20),
(16, 1, 16, 16, 20),

-- RT-HN-NE Stops (Stores 17 to 33, sequenced from closest to furthest)
(17, 2, 17, 1, 15),
(18, 2, 18, 2, 15),
(19, 2, 19, 3, 15),
(20, 2, 20, 4, 15),
(21, 2, 21, 5, 15),
(22, 2, 22, 6, 15),
(23, 2, 23, 7, 20),
(24, 2, 24, 8, 20),
(25, 2, 25, 9, 20),
(26, 2, 26, 10, 20),
(27, 2, 27, 11, 20),
(28, 2, 28, 12, 20),
(29, 2, 29, 13, 20),
(30, 2, 30, 14, 20),
(31, 2, 31, 15, 20),
(32, 2, 32, 16, 20),
(33, 2, 33, 17, 20),

-- RT-HN-EAST Stops (Stores 34 to 50, sequenced from closest to furthest)
(34, 3, 34, 1, 15),
(35, 3, 35, 2, 15),
(36, 3, 36, 3, 15),
(37, 3, 37, 4, 15),
(38, 3, 38, 5, 15),
(39, 3, 39, 6, 15),
(40, 3, 40, 7, 15),
(41, 3, 41, 8, 15),
(42, 3, 42, 9, 20),
(43, 3, 43, 10, 20),
(44, 3, 44, 11, 20),
(45, 3, 45, 12, 20),
(46, 3, 46, 13, 20),
(47, 3, 47, 14, 20),
(48, 3, 48, 15, 20),
(49, 3, 49, 16, 20),
(50, 3, 50, 17, 20);

SET FOREIGN_KEY_CHECKS = 1;
