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
     * 더블 세이브 패턴 전용 — placeholder shortKey로 insert 후 실제 id 기반 키로 교체.
     *
     * <p><b>호출 허용:</b> {@code ShortenService}에서만 (생성 직후 1회).<br>
     * <b>호출 금지:</b> 컨트롤러, 다른 서비스, 마이그레이션 코드, 테스트 fixture 외 위치.<br>
     * 컴파일러로 막을 수 없어 컨벤션으로 보호한다. 위반 시 PR 리뷰에서 reject.
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

    /**
     * 클릭 수 +1.
     *
     * <p><b>동시성 한계:</b> 단순 `clickCount++`라 두 트랜잭션이 동시에 읽고 쓰면
     * lost update가 발생한다. 1단계에선 리다이렉트 경로에서 호출하지 않아 문제없지만,
     * 4단계에서 통계를 켜면 <b>로그 기반 집계</b>로 전환해 이 메서드 의존을 제거한다.
     * (DB-level `UPDATE ... SET click_count = click_count + 1` 또는 Redis INCR 대안)
     */
    public void incrementClickCount() {
        this.clickCount++;
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
