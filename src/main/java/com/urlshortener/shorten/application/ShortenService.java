package com.urlshortener.shorten.application;

import com.urlshortener.cache.UrlCache;
import com.urlshortener.domain.ShortKey;
import com.urlshortener.persistence.Url;
import com.urlshortener.persistence.UrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class ShortenService {

    /** short_key 충돌 시 다른 랜덤 키로 재시도하는 최대 횟수. 키공간 62^7이라 충돌 자체가 희박. */
    private static final int MAX_RETRY = 5;

    private final UrlRepository urlRepository;
    private final UrlCache l1;
    private final UrlCache l2;

    public ShortenService(
            UrlRepository urlRepository,
            @Qualifier("l1Cache") UrlCache l1,
            @Qualifier("l2Cache") UrlCache l2
    ) {
        this.urlRepository = urlRepository;
        this.l1 = l1;
        this.l2 = l2;
    }

    @Transactional
    public ShortKey shorten(String longUrl) {
        return urlRepository.findByLongUrl(longUrl)
                .map(existing -> {
                    log.debug("dedup hit: longUrl already shortened to {}", existing.shortKey());
                    return existing.shortKey();
                })
                .orElseGet(() -> createNewShortened(longUrl));
    }

    /**
     * 랜덤 키로 INSERT 1회. 이전엔 id 기반 키라 INSERT 후 UPDATE(쓰기 2회)가 필요했지만,
     * 키를 id에 의존하지 않는 랜덤으로 발급해 쓰기를 1회로 줄였다.
     * UNIQUE 충돌은 두 가지를 구분한다:
     *  - long_url 충돌: 다른 트랜잭션이 같은 URL을 먼저 단축 → 그 기존 키를 반환(클라이언트엔 성공)
     *  - short_key 충돌: 같은 랜덤 키가 이미 있음(희박) → 다른 키로 재시도
     */
    private ShortKey createNewShortened(String longUrl) {
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            ShortKey key = ShortKey.random(ThreadLocalRandom.current());
            try {
                Url saved = urlRepository.save(Url.of(longUrl, key));
                writeThrough(key, saved.longUrl());
                log.debug("shortened: key={}", key);
                return key;
            } catch (DataIntegrityViolationException ex) {
                // 같은 longUrl이 이미 있으면 동시 단축 → 기존 키 반환(클라이언트엔 성공으로 보임)
                Optional<ShortKey> existing = urlRepository.findByLongUrl(longUrl).map(Url::shortKey);
                if (existing.isPresent()) {
                    log.info("concurrent shortening, returning existing key for: {}", longUrl);
                    return existing.get();
                }
                // longUrl이 없으면 short_key 충돌 → 다른 랜덤 키로 재시도
                log.debug("short_key collision, retrying ({}/{})", attempt, MAX_RETRY);
            }
        }
        throw new ShortKeyGenerationException(longUrl, MAX_RETRY);
    }

    /** L1·L2에 매핑을 미리 적재해 첫 조회부터 캐시 hit이 되게 한다(write-through). 캐시 장애는 무시. */
    private void writeThrough(ShortKey key, String longUrl) {
        try {
            l1.put(key, longUrl);
        } catch (RuntimeException ex) {
            log.warn("L1 write-through failed (ignored): {}", ex.getMessage());
        }
        try {
            l2.put(key, longUrl);
        } catch (RuntimeException ex) {
            log.warn("L2 write-through failed (ignored): {}", ex.getMessage());
        }
    }
}
