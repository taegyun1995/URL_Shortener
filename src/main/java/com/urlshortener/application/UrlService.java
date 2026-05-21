package com.urlshortener.application;

import com.urlshortener.domain.Base62Encoder;
import com.urlshortener.domain.ShortKey;
import com.urlshortener.infrastructure.persistence.entity.Url;
import com.urlshortener.infrastructure.persistence.repository.UrlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
public class UrlService {

    private static final String BASE62_ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int SHORT_KEY_LENGTH = 7;

    private final UrlRepository urlRepository;
    private final Base62Encoder base62Encoder;
    private final SecureRandom random = new SecureRandom();

    public UrlService(UrlRepository urlRepository, Base62Encoder base62Encoder) {
        this.urlRepository = urlRepository;
        this.base62Encoder = base62Encoder;
    }

    @Transactional
    public ShortKey shorten(String longUrl) {
        // 1) 중복 URL 검사
        return urlRepository.findByLongUrl(longUrl)
                .map(Url::shortKey)
                .orElseGet(() -> createNewShortened(longUrl));
    }

    @Transactional(readOnly = true)
    public String resolve(ShortKey shortKey) {
        return urlRepository.findByShortKey(shortKey)
                .map(Url::longUrl)
                .orElseThrow(() -> new ShortKeyNotFoundException(shortKey));
    }

    private ShortKey createNewShortened(String longUrl) {
        // 2) placeholder 키로 insert → id 확보
        ShortKey placeholder = generatePlaceholderKey();
        Url url = Url.of(longUrl, placeholder);
        Url saved = urlRepository.save(url);

        // 3) 실제 id를 Base62 인코딩 → 7자 패딩
        String encoded = base62Encoder.encode(saved.id());
        ShortKey finalKey = ShortKey.of(padTo7(encoded));

        // 4) shortKey 교체 (JPA dirty check로 update)
        saved.assignShortKey(finalKey);

        return finalKey;
    }

    private ShortKey generatePlaceholderKey() {
        StringBuilder sb = new StringBuilder(SHORT_KEY_LENGTH);
        for (int i = 0; i < SHORT_KEY_LENGTH; i++) {
            sb.append(BASE62_ALPHABET.charAt(random.nextInt(BASE62_ALPHABET.length())));
        }
        return ShortKey.of(sb.toString());
    }

    private String padTo7(String encoded) {
        if (encoded.length() >= SHORT_KEY_LENGTH) {
            return encoded;
        }
        return "0".repeat(SHORT_KEY_LENGTH - encoded.length()) + encoded;
    }
}
