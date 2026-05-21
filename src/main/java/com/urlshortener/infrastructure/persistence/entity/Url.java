package com.urlshortener.infrastructure.persistence.entity;

import com.urlshortener.domain.ShortKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "urls")
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "long_url", nullable = false, length = 2048)
    private String longUrl;

    @Column(name = "short_key", nullable = false, unique = true, length = 7)
    private ShortKey shortKey;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "click_count", nullable = false)
    private long clickCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Url() {
        // JPA 전용
    }

    private Url(String longUrl, ShortKey shortKey, Instant expiresAt) {
        this.longUrl = longUrl;
        this.shortKey = shortKey;
        this.expiresAt = expiresAt;
        this.clickCount = 0L;
    }

    public static Url of(String longUrl, ShortKey shortKey) {
        return of(longUrl, shortKey, null);
    }

    public static Url of(String longUrl, ShortKey shortKey, Instant expiresAt) {
        if (longUrl == null) {
            throw new IllegalArgumentException("longUrl must not be null");
        }
        if (shortKey == null) {
            throw new IllegalArgumentException("shortKey must not be null");
        }
        String trimmed = longUrl.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("longUrl must not be blank");
        }
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            throw new IllegalArgumentException("longUrl must start with http:// or https://: " + longUrl);
        }
        return new Url(longUrl, shortKey, expiresAt);
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long id() {
        return id;
    }

    public String longUrl() {
        return longUrl;
    }

    public ShortKey shortKey() {
        return shortKey;
    }

    /**
     * 더블 세이브 패턴 — placeholder shortKey로 insert 후, 실제 id 기반 키로 교체.
     * Service 외부에서 호출 금지.
     */
    public void assignShortKey(ShortKey newShortKey) {
        if (newShortKey == null) {
            throw new IllegalArgumentException("shortKey must not be null");
        }
        this.shortKey = newShortKey;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public long clickCount() {
        return clickCount;
    }

    public void incrementClickCount() {
        this.clickCount++;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !now.isBefore(expiresAt);
    }
}
