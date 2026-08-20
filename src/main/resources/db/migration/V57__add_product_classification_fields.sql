-- V57__add_product_classification_fields.sql
-- Add product classification/attribute fields requested for reporting & Excel export:
--   Thương hiệu (Brand), Nhóm hàng (Product Group), Loại hàng hóa (Product Type),
--   Dung tích (L) / KL giặt (kg) (single numeric attribute — liquid volume in litres for
--   some product types, washing capacity in kg for washing-machine-type products).
-- All nullable: existing seeded products (V7) have none of this data, and it is optional
-- metadata, not required for any capacity/dispatch calculation.

ALTER TABLE products
    ADD COLUMN brand           VARCHAR(100)  NULL AFTER product_name,
    ADD COLUMN product_group   VARCHAR(100)  NULL AFTER brand,
    ADD COLUMN product_type    VARCHAR(100)  NULL AFTER product_group,
    ADD COLUMN capacity_value  DECIMAL(10,2) NULL AFTER product_type;
