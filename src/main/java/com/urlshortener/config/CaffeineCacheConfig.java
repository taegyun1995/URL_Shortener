package com.urlshortener.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 1단계 in-process 캐시 (Caffeine).
 * <p>
 * 2단계에서 Redis L2가 추가되어 다단계 캐시로 확장된다.
 * L1 일관성은 도메인이 사실상 immutable(shortKey → longUrl 매핑 불변)이라 약하다 —
 * scale-out 시 인스턴스별 stale 위험이 일반 도메인보다 작다는 논증으로 닫는다. scale-out 실측은 스코프 밖(후속).
 * <p>
 * 실험용 토글: {@code app.cache.caffeine.enabled=false}로 끄면 NoOpCacheManager를 주입해 캐시 없는 baseline 측정 가능.
 */
@Configuration
@EnableCaching
public class CaffeineCacheConfig {

    public static final String URLS_CACHE = "urls";

    @Bean
    @ConditionalOnProperty(name = "app.cache.caffeine.enabled", havingValue = "true", matchIfMissing = true)
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(URLS_CACHE);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats());
        return manager;
    }

    @Bean
    @ConditionalOnProperty(name = "app.cache.caffeine.enabled", havingValue = "false")
    public CacheManager noOpCacheManager() {
        return new NoOpCacheManager();
    }
}
