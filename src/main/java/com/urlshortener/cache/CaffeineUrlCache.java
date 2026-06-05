package com.urlshortener.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.urlshortener.domain.ShortKey;
import org.springframework.stereotype.Component;

/**
 * L1 캐시 — Caffeine in-process.
 * <p>
 * negative(없는 키)는 {@link UrlCache#NEGATIVE} sentinel로 저장한다. shortKey → longUrl
 * 매핑이 사실상 immutable이라 L1 stale 위험은 약하다(§7, anti-scope 재검토 참고).
 */
@Component("l1Cache")
public class CaffeineUrlCache implements UrlCache {

    private final Cache<String, String> cache;

    public CaffeineUrlCache(Cache<String, String> l1NativeCache) {
        this.cache = l1NativeCache;
    }

    @Override
    public Lookup get(ShortKey shortKey) {
        String value = cache.getIfPresent(shortKey.value());
        if (value == null) {
            return Lookup.miss();
        }
        return NEGATIVE.equals(value) ? Lookup.negativeHit() : Lookup.hit(value);
    }

    @Override
    public void put(ShortKey shortKey, String longUrl) {
        cache.put(shortKey.value(), longUrl);
    }

    @Override
    public void putNegative(ShortKey shortKey) {
        cache.put(shortKey.value(), NEGATIVE);
    }
}
