package com.urlshortener.infrastructure.persistence.repository;

import com.urlshortener.domain.ShortKey;
import com.urlshortener.infrastructure.persistence.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortKey(ShortKey shortKey);

    Optional<Url> findByLongUrl(String longUrl);
}
