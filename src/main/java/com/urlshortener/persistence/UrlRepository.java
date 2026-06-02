package com.urlshortener.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.urlshortener.domain.ShortKey;

public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortKey(ShortKey shortKey);

    Optional<Url> findByLongUrl(String longUrl);
}
