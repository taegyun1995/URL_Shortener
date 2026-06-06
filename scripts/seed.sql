-- 부하 테스트용 더미 URL 10,000건 삽입.
-- shortKey는 LPAD(CONV(id, 10, 36), 7, '0') 형식.
-- (base 36은 Base62 알파벳의 부분집합이라 ShortKey 검증 통과)
--
-- 실행: docker exec -i url-shortener-mysql mysql -uurlshortener -purlshortener_pass urlshortener < scripts/seed.sql

USE urlshortener;

-- 중복 실행 방지: 이미 데이터 있으면 skip
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS seed_urls()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE current_count INT;

    SELECT COUNT(*) INTO current_count FROM urls;
    IF current_count >= 10000 THEN
        SELECT CONCAT('Skipping seed: urls table already has ', current_count, ' rows') AS msg;
    ELSE
        WHILE i <= 10000 DO
            INSERT IGNORE INTO urls (short_key, long_url, created_at)
            VALUES (
                LPAD(CONV(i + 1000000, 10, 36), 7, '0'),    -- id offset으로 더 다양한 키
                CONCAT('https://example.com/seeded/page/', i),
                NOW()
            );
            SET i = i + 1;
        END WHILE;
        SELECT CONCAT('Seeded ', i - 1, ' urls') AS msg;
    END IF;
END //
DELIMITER ;

CALL seed_urls();
DROP PROCEDURE seed_urls;
