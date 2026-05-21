package com.urlshortener.domain;

import java.time.Instant;

public final class Url {

    private final String longUrl;
    private final ShortKey shortKey;
    private final Instant expiresAt;
    private long clickCount;

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

    public String longUrl() {
        return longUrl;
    }

    public ShortKey shortKey() {
        return shortKey;
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
