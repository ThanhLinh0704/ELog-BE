-- Triển khai bảng lưu trữ refresh token phục vụ thu hồi session & logout

CREATE TABLE refresh_tokens (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    token       VARCHAR(255)  NOT NULL,
    user_id     BIGINT        NOT NULL,
    expiry_date DATETIME      NOT NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);