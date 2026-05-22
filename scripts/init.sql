-- MySQL 컨테이너 최초 기동 시 자동 실행됨 (/docker-entrypoint-initdb.d/)
-- 데이터 모델 상세: docs/data-model.md

CREATE DATABASE IF NOT EXISTS urlshortener
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE urlshortener;

-- urls: 단축 URL 원본
-- short_key는 COLLATE utf8mb4_bin (case-sensitive) 필수.
-- Base62 알파벳에 대소문자가 모두 있어, 기본 utf8mb4_unicode_ci로는 'Bn' == 'BN'으로 취급돼 충돌.
CREATE TABLE IF NOT EXISTS urls (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_key   VARCHAR(7)    NOT NULL COLLATE utf8mb4_bin UNIQUE,
    long_url    VARCHAR(2048) NOT NULL,
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    expires_at  DATETIME      NULL,
    click_count BIGINT        DEFAULT 0,

    INDEX idx_short_key (short_key),
    INDEX idx_long_url  (long_url(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- click_logs: 리다이렉트 클릭 이력 (4단계에서 본격 사용)
CREATE TABLE IF NOT EXISTS click_logs (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_key  VARCHAR(7)    NOT NULL,
    clicked_at DATETIME      DEFAULT CURRENT_TIMESTAMP,
    ip         VARCHAR(45),
    user_agent VARCHAR(512),
    referer    VARCHAR(2048),

    INDEX idx_short_key_time (short_key, clicked_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
