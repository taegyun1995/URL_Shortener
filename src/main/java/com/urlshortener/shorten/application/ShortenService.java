package com.urlshortener.shorten.application;

import com.urlshortener.cache.UrlCache;
import com.urlshortener.domain.ShortKey;
import com.urlshortener.persistence.Url;
import com.urlshortener.persistence.UrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.sqids.Sqids;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class ShortenService {

    private final UrlRepository urlRepository;
    private final Sqids sqids;
    private final UrlCache l1;
    private final UrlCache l2;

    public ShortenService(
            UrlRepository urlRepository,
            Sqids sqids,
            @Qualifier("l1Cache") UrlCache l1,
            @Qualifier("l2Cache") UrlCache l2
    ) {
        this.urlRepository = urlRepository;
        this.sqids = sqids;
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
     * 신규 단축 키 발급. shortKey 생성에 id가 필요하나 id는 INSERT 후 정해지므로 INSERT 후 UPDATE한다.
     * 1) placeholder 키로 INSERT → DB가 auto-increment id 부여
     * 2) 받은 id를 Sqids로 인코딩 → 실제 키
     * 3) entity의 shortKey 교체 → JPA 변경 감지(dirty checking)가 커밋 시 UPDATE 발행
     */
    private ShortKey createNewShortened(String longUrl) {
        try {
            ShortKey placeholder = ShortKey.random(ThreadLocalRandom.current());
            Url saved = urlRepository.save(Url.of(longUrl, placeholder));

            ShortKey finalKey = ShortKey.of(sqids.encode(List.of(saved.id())));
            saved.assignShortKey(finalKey);

            // 방금 만든 매핑을 캐시에 미리 적재해 첫 조회부터 캐시 hit이 되게 한다(write-through).
            // 캐시 적재 실패가 단축 자체를 막으면 안 되므로 예외는 삼킨다.
            writeThrough(finalKey, saved.longUrl());

            log.debug("shortened: id={} key={}", saved.id(), finalKey);
            return finalKey;
        } catch (DataIntegrityViolationException ex) {
            // long_url UNIQUE 위반 — 다른 트랜잭션이 같은 URL을 먼저 INSERT.
            // 현재 TX는 rollback. 클라이언트는 재시도 시 기존 키를 받음.
            log.info("race condition on shorten: {}", longUrl);
            throw new ConcurrentShorteningException(longUrl, ex);
        }
    }

    /** L1·L2에 매핑을 미리 적재. 캐시 장애는 무시(단축 응답을 막지 않는다). */
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
