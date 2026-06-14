-- 1. Thêm cột email vào bảng users (cho phép tạm thời NULL để tránh lỗi trùng lặp khi khởi tạo)
ALTER TABLE users ADD COLUMN email VARCHAR(100) NULL AFTER full_name;

-- 2. Cập nhật email duy nhất cho các user đã có sẵn trong hệ thống dựa trên username của họ
UPDATE users SET email = CONCAT(username, '@elog.vn');

-- 3. Sau khi đã điền đủ email, chuyển cột email thành NOT NULL và thiết lập ràng buộc UNIQUE
ALTER TABLE users MODIFY COLUMN email VARCHAR(100) NOT NULL;
ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email);

-- 4. Tạo bảng trung gian user_roles cho mối quan hệ Many-to-Many
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Chuyển dữ liệu role cũ sang bảng user_roles
INSERT INTO user_roles (user_id, role_id)
SELECT id, role_id FROM users;

-- 6. Xóa cột role_id dư thừa ở bảng users
ALTER TABLE users DROP FOREIGN KEY fk_users_role;
ALTER TABLE users DROP COLUMN role_id;

-- 7. Cập nhật tên vai trò ADMIN thành SYSTEM_ADMIN
UPDATE roles SET name = 'SYSTEM_ADMIN' WHERE name = 'ADMIN';
