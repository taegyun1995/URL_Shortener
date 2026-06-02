package com.urlshortener.shorten.application;

import com.urlshortener.domain.ShortKey;
import com.urlshortener.persistence.Url;
import com.urlshortener.persistence.UrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.sqids.Sqids;
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

    public ShortenService(UrlRepository urlRepository, Sqids sqids) {
        this.urlRepository = urlRepository;
        this.sqids = sqids;
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
     * INSERT-then-UPDATE 패턴으로 신규 단축 키 발급.
     * 1) placeholder 키로 INSERT → DB가 auto-increment id 부여
     * 2) 받은 id를 Sqids로 인코딩 → 실제 키
     * 3) entity의 shortKey 교체 → JPA dirty check가 트랜잭션 종료 시 UPDATE 발행
     */
    private ShortKey createNewShortened(String longUrl) {
        try {
            ShortKey placeholder = ShortKey.random(ThreadLocalRandom.current());
            Url saved = urlRepository.save(Url.of(longUrl, placeholder));

            ShortKey finalKey = ShortKey.of(sqids.encode(List.of(saved.id())));
            saved.assignShortKey(finalKey);

            log.debug("shortened: id={} key={}", saved.id(), finalKey);
            return finalKey;
        } catch (DataIntegrityViolationException ex) {
            // long_url UNIQUE 위반 — 다른 트랜잭션이 같은 URL을 먼저 INSERT.
            // 현재 TX는 rollback. 클라이언트는 재시도 시 기존 키를 받음.
            log.info("race condition on shorten: {}", longUrl);
            throw new ConcurrentShorteningException(longUrl, ex);
        }
    }
}
