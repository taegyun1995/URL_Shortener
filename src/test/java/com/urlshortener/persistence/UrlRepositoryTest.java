package com.urlshortener.persistence;

import com.urlshortener.domain.ShortKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
class UrlRepositoryTest {

    @Autowired
    private UrlRepository urlRepository;

    @Test
    void 저장_후_shortKey로_조회된다() {
        ShortKey shortKey = ShortKey.of("abc1234");
        Url url = Url.of("https://example.com/long", shortKey);

        urlRepository.save(url);

        Optional<Url> found = urlRepository.findByShortKey(shortKey);

        assertThat(found).isPresent();
        assertThat(found.get().longUrl()).isEqualTo("https://example.com/long");
        assertThat(found.get().shortKey()).isEqualTo(shortKey);
        assertThat(found.get().id()).isNotNull();
        assertThat(found.get().createdAt()).isNotNull();
    }

    @Test
    void longUrl로_Url을_조회한다() {
        Url url = Url.of("https://example.com/another", ShortKey.of("def5678"));
        urlRepository.save(url);

        Optional<Url> found = urlRepository.findByLongUrl("https://example.com/another");

        assertThat(found).isPresent();
        assertThat(found.get().shortKey().value()).isEqualTo("def5678");
    }

    @Test
    void 없는_shortKey_조회시_빈_Optional을_반환한다() {
        Optional<Url> found = urlRepository.findByShortKey(ShortKey.of("zzzzzzz"));
        assertThat(found).isEmpty();
    }
}
