package com.urlshortener.persistence;

import com.urlshortener.domain.ShortKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Set;

@Entity
@Table(name = "urls")
public class Url {

    /** RFC 7230 권장 URL 최대 길이. 브라우저·서버 호환성 임계. */
    public static final int LONG_URL_MAX_LENGTH = 2048;

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "long_url", nullable = false, length = LONG_URL_MAX_LENGTH, unique = true)
    private String longUrl;

    @Column(name = "short_key", nullable = false, unique = true, length = ShortKey.LENGTH)
    private ShortKey shortKey;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Url() {
        // JPA 전용
    }

    private Url(String longUrl, ShortKey shortKey, Instant expiresAt) {
        this.longUrl = longUrl;
        this.shortKey = shortKey;
        this.expiresAt = expiresAt;
    }

    public static Url of(String longUrl, ShortKey shortKey) {
        return of(longUrl, shortKey, null);
    }

    public static Url of(String longUrl, ShortKey shortKey, Instant expiresAt) {
        if (shortKey == null) {
            throw new IllegalArgumentException("shortKey must not be null");
        }
        // 검증을 통과한 정규화(trim된) URL을 저장한다 — 입력 그대로가 아니라.
        String normalized = normalizeAndValidate(longUrl);
        return new Url(normalized, shortKey, expiresAt);
    }

    /**
     * longUrl을 trim하고 URI로 파싱해 스킴·호스트를 검증한다.
     * naive `startsWith` 대신 `java.net.URI`를 써서 `http://`(호스트 없음),
     * `javascript:`, protocol-relative(`//host`) 같은 변형을 모두 걸러낸다.
     *
     * @return 검증을 통과한 trim된 URL (DB에 저장될 값)
     */
    private static String normalizeAndValidate(String longUrl) {
        if (longUrl == null) {
            throw new IllegalArgumentException("longUrl must not be null");
        }
        String trimmed = longUrl.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("longUrl must not be blank");
        }
        if (trimmed.length() > LONG_URL_MAX_LENGTH) {
            throw new IllegalArgumentException("longUrl exceeds " + LONG_URL_MAX_LENGTH + " chars: " + trimmed.length());
        }

        final URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("longUrl is not a valid URI: " + longUrl, e);
        }

        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new IllegalArgumentException("longUrl must use http or https scheme: " + longUrl);
        }
        if (uri.getHost() == null || uri.getHost().isEmpty()) {
            throw new IllegalArgumentException("longUrl must have a host: " + longUrl);
        }
        return trimmed;
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
     * placeholder shortKey로 INSERT한 뒤, DB가 부여한 id로 만든 실제 키로 교체한다.
     * shortKey 생성에 id가 필요하지만 id는 INSERT 후에야 정해지므로, 한 번 더 쓰는 구조다.
     *
     * <p>{@code ShortenService}의 생성 직후에만 호출한다. 컨트롤러·다른 서비스에서는
     * 호출하지 않는다(컴파일러로 막을 수 없어 컨벤션으로 보호).
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

    public boolean isExpired(Instant now) {
        return expiresAt != null && !now.isBefore(expiresAt);
    }

    /**
     * JPA Entity equality: id 기반.
     * - id가 null인 transient 상태에선 reference equality만 같음
     * - hashCode는 proxy 호환을 위해 클래스 기반 고정값 사용 (Vlad Mihalcea 권장 패턴)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Url other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
