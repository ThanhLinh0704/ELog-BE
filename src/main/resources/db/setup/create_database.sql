-- ============================================================
-- ELog Delivery Management System
-- T-01 Step 1: Create Database
-- Run this ONCE manually as a MySQL admin user
-- before starting the Spring Boot application.
-- ============================================================

CREATE DATABASE IF NOT EXISTS elog_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Create application user (replace password in production)
-- Skip this block if you use root locally for development.
-- CREATE USER IF NOT EXISTS 'elog_app'@'localhost' IDENTIFIED BY 'elog_dev_2025!';
-- GRANT ALL PRIVILEGES ON elog_db.* TO 'elog_app'@'localhost';
-- FLUSH PRIVILEGES;

USE elog_db;

-- Verify character set
SHOW VARIABLES LIKE 'character_set_database';
SHOW VARIABLES LIKE 'collation_database';
