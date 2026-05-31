package com.urlshortener.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShortKeyTest {

    // === ShortKey.of(String) ===

    @Test
    void 유효한_문자열로_ShortKey를_생성한다() {
        ShortKey key = ShortKey.of("abc1234");
        assertThat(key.value()).isEqualTo("abc1234");
    }

    @Test
    void null이면_예외를_던진다() {
        assertThatThrownBy(() -> ShortKey.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "abcdef", "abcdefgh", "abcdefghijk"})
    void 길이가_7이_아니면_예외를_던진다(String invalid) {
        assertThatThrownBy(() -> ShortKey.of(invalid))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc!234", "abc 234", "abc/234", "한글일곱자글자"})
    void 알파벳_외_문자가_있으면_예외를_던진다(String invalid) {
        assertThatThrownBy(() -> ShortKey.of(invalid))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // === ShortKey.random(Random) — placeholder 생성 ===

    @Test
    void random은_LENGTH_길이의_키를_반환한다() {
        ShortKey key = ShortKey.random(ThreadLocalRandom.current());
        assertThat(key.value()).hasSize(ShortKey.LENGTH);
    }

    @Test
    void random은_알파벳_안의_문자만_사용한다() {
        ShortKey key = ShortKey.random(ThreadLocalRandom.current());
        assertThat(key.value()).matches("[" + ShortKey.ALPHABET + "]+");
    }

    @Test
    void random은_연속_호출_시_서로_다른_값을_반환한다() {
        // 확률적: 62^7 = 3.5조라 같은 값이 나올 확률 거의 0
        ShortKey k1 = ShortKey.random(ThreadLocalRandom.current());
        ShortKey k2 = ShortKey.random(ThreadLocalRandom.current());
        assertThat(k1).isNotEqualTo(k2);
    }

    // === 동등성 ===

    @Test
    void 같은_값으로_생성하면_동등하다() {
        ShortKey k1 = ShortKey.of("abc1234");
        ShortKey k2 = ShortKey.of("abc1234");
        assertThat(k1).isEqualTo(k2);
        assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
    }

    @Test
    void toString은_value를_반환한다() {
        ShortKey key = ShortKey.of("abc1234");
        assertThat(key).hasToString("abc1234");
    }
}
