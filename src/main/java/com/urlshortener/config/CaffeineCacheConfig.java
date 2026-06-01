package com.urlshortener.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
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
 * 캐시는 항상 켜져 있다. 운영 중 끌 이유가 없어 on/off 토글을 두지 않는다(실수로 끄는 사고 방지).
 * 캐시 효과(+100%)는 도입 시점에 NoOp 캐시와 일회성으로 비교 측정해 검증했고, 이후 무조건 ON으로 고정했다.
 */
@Configuration
@EnableCaching
public class CaffeineCacheConfig {

    public static final String URLS_CACHE = "urls";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(URLS_CACHE);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats());
        return manager;
    }
}
