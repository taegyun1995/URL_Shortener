package com.urlshortener.cache;

import com.urlshortener.domain.ShortKey;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * L2 캐시 — Redis (분산).
 * <p>
 * positive는 긴 TTL(1h), negative는 짧은 TTL(5m)로 저장한다.
 * negative는 "없던 키가 나중에 생길" 가능성 때문에 짧게 둔다(stale negative 방지).
 */
@Component("l2Cache")
public class RedisUrlCache implements UrlCache {

    private static final String KEY_PREFIX = "url:";
    private static final Duration POSITIVE_TTL = Duration.ofHours(1);
    private static final Duration NEGATIVE_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redis;

    public RedisUrlCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Lookup get(ShortKey shortKey) {
        String value = redis.opsForValue().get(redisKey(shortKey));
        if (value == null) {
            return Lookup.miss();
        }
        return NEGATIVE.equals(value) ? Lookup.negativeHit() : Lookup.hit(value);
    }

    @Override
    public void put(ShortKey shortKey, String longUrl) {
        redis.opsForValue().set(redisKey(shortKey), longUrl, POSITIVE_TTL);
    }

    @Override
    public void putNegative(ShortKey shortKey) {
        redis.opsForValue().set(redisKey(shortKey), NEGATIVE, NEGATIVE_TTL);
    }

    private String redisKey(ShortKey shortKey) {
        return KEY_PREFIX + shortKey.value();
    }
}
