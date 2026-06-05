package com.urlshortener.redirect.application;

import com.urlshortener.domain.ShortKey;
import com.urlshortener.persistence.Url;
import com.urlshortener.persistence.UrlRepository;
import com.urlshortener.redirect.application.UrlCache.Lookup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다단계 캐시 기반 리다이렉트 조회.
 * <p>
 * 조회 순서: L1(Caffeine) → L2(Redis) → DB. 하위 계층에서 찾으면 상위 계층을 역순으로 채운다.
 * 없는 키는 negative cache로 표시해 무한 DB 조회(§7 Negative Cache)를 막는다.
 */
@Service
@Slf4j
public class RedirectService {

    private final UrlCache l1;
    private final UrlCache l2;
    private final UrlRepository urlRepository;

    public RedirectService(
            @Qualifier("l1Cache") UrlCache l1,
            @Qualifier("l2Cache") UrlCache l2,
            UrlRepository urlRepository
    ) {
        this.l1 = l1;
        this.l2 = l2;
        this.urlRepository = urlRepository;
    }

    @Transactional(readOnly = true)
    public String resolve(ShortKey shortKey) {
        // L1
        Lookup l1Result = l1.get(shortKey);
        if (!l1Result.isMiss()) {
            return unwrap(l1Result, shortKey);
        }

        // L2 — Redis 장애 시 miss로 간주하고 DB로 폴백 (캐시는 가용성을 막지 않는다)
        Lookup l2Result = l2Get(shortKey);
        if (!l2Result.isMiss()) {
            if (!l2Result.negative()) {
                l1.put(shortKey, l2Result.value());
            } else {
                l1.putNegative(shortKey);
            }
            return unwrap(l2Result, shortKey);
        }

        // DB
        return urlRepository.findByShortKey(shortKey)
                .map(Url::longUrl)
                .map(longUrl -> {
                    l2Put(shortKey, longUrl);
                    l1.put(shortKey, longUrl);
                    return longUrl;
                })
                .orElseThrow(() -> {
                    l2PutNegative(shortKey);
                    l1.putNegative(shortKey);
                    return new ShortKeyNotFoundException(shortKey);
                });
    }

    // --- L2(Redis) 접근은 장애에 안전하게 감싼다. 캐시 실패가 리다이렉트를 막으면 안 된다. ---

    private Lookup l2Get(ShortKey shortKey) {
        try {
            return l2.get(shortKey);
        } catch (RuntimeException ex) {
            log.warn("L2 get failed, falling back to DB: {}", ex.getMessage());
            return Lookup.miss();
        }
    }

    private void l2Put(ShortKey shortKey, String longUrl) {
        try {
            l2.put(shortKey, longUrl);
        } catch (RuntimeException ex) {
            log.warn("L2 put failed (ignored): {}", ex.getMessage());
        }
    }

    private void l2PutNegative(ShortKey shortKey) {
        try {
            l2.putNegative(shortKey);
        } catch (RuntimeException ex) {
            log.warn("L2 putNegative failed (ignored): {}", ex.getMessage());
        }
    }

    /** 캐시 hit 결과를 값으로 풀거나, negative면 예외로 변환. */
    private String unwrap(Lookup lookup, ShortKey shortKey) {
        if (lookup.negative()) {
            throw new ShortKeyNotFoundException(shortKey);
        }
        return lookup.value();
    }
}
