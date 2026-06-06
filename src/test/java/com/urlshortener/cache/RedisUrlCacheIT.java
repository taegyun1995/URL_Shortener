package com.urlshortener.cache;

import com.urlshortener.cache.UrlCache.Lookup;
import com.urlshortener.domain.ShortKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisUrlCache 통합 테스트 — 실제 Redis 컨테이너로 검증.
 * <p>
 * 단위 테스트(mock)로는 가릴 수 없는 부분을 본다: Redis 왕복, negative sentinel
 * 직렬화/복원, TTL 적용 여부. Redis 자동구성 + RedisUrlCache만 띄워 가볍게 검증한다.
 */
@SpringBootTest(classes = {RedisAutoConfiguration.class, RedisUrlCache.class})
@Testcontainers
class RedisUrlCacheIT {

    @Container
    @ServiceConnection
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
                    .withExposedPorts(6379);

    @Autowired
    private RedisUrlCache cache;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ShortKey key = ShortKey.of("abc1234");

    @BeforeEach
    void flush() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void put한_값을_get으로_되읽는다() {
        cache.put(key, "https://example.com/long");

        Lookup result = cache.get(key);

        assertThat(result.present()).isTrue();
        assertThat(result.negative()).isFalse();
        assertThat(result.value()).isEqualTo("https://example.com/long");
    }

    @Test
    void 없는_키는_miss를_반환한다() {
        Lookup result = cache.get(ShortKey.of("zzzzzzz"));

        assertThat(result.isMiss()).isTrue();
    }

    @Test
    void putNegative한_키는_negativeHit을_반환한다() {
        cache.putNegative(key);

        Lookup result = cache.get(key);

        assertThat(result.present()).isTrue();
        assertThat(result.negative()).isTrue();
        assertThat(result.value()).isNull();
    }

    @Test
    void positive는_긴_TTL_negative는_짧은_TTL로_저장된다() {
        cache.put(key, "https://example.com");
        Long positiveTtl = redisTemplate.getExpire("url:" + key.value());

        ShortKey negKey = ShortKey.of("defGHI7");
        cache.putNegative(negKey);
        Long negativeTtl = redisTemplate.getExpire("url:" + negKey.value());

        // positive 1h(3600s), negative 5m(300s) — 경계 오차 감안
        assertThat(positiveTtl).isGreaterThan(3000).isLessThanOrEqualTo(3600);
        assertThat(negativeTtl).isGreaterThan(200).isLessThanOrEqualTo(300);
    }
}
