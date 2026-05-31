package com.urlshortener.infrastructure.persistence.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.urlshortener.domain.ShortKey;
import com.urlshortener.infrastructure.persistence.entity.Url;

public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortKey(ShortKey shortKey);

    Optional<Url> findByLongUrl(String longUrl);
}
