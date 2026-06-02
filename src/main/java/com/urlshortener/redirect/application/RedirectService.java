package com.urlshortener.redirect.application;

import com.urlshortener.config.CaffeineCacheConfig;
import com.urlshortener.domain.ShortKey;
import com.urlshortener.persistence.Url;
import com.urlshortener.persistence.UrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class RedirectService {

    private final UrlRepository urlRepository;

    public RedirectService(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CaffeineCacheConfig.URLS_CACHE, key = "#shortKey.value()")
    public String resolve(ShortKey shortKey) {
        return urlRepository.findByShortKey(shortKey)
                .map(Url::longUrl)
                .orElseThrow(() -> new ShortKeyNotFoundException(shortKey));
    }
}
