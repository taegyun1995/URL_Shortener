package com.urlshortener.domain;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

public final class ShortKey {

    public static final int LENGTH = 7;

    public static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int ASCII_RANGE = 128;
    private static final boolean[] VALID_CHAR_TABLE = buildValidCharTable();

    private static boolean[] buildValidCharTable() {
        boolean[] table = new boolean[ASCII_RANGE];
        Arrays.fill(table, false);
        for (int i = 0; i < ALPHABET.length(); i++) {
            table[ALPHABET.charAt(i)] = true;
        }
        return table;
    }

    private static boolean isValidChar(char c) {
        return c < ASCII_RANGE && VALID_CHAR_TABLE[c];
    }

    private final String value;

    private ShortKey(String value) {
        this.value = value;
    }

    public static ShortKey of(String value) {
        if (value == null) {
            throw new IllegalArgumentException("ShortKey value must not be null");
        }
        if (value.length() != LENGTH) {
            throw new IllegalArgumentException("ShortKey must be exactly " + LENGTH + " characters: " + value);
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!isValidChar(c)) {
                throw new IllegalArgumentException("ShortKey contains invalid character: '" + c + "' in: " + value);
            }
        }
        return new ShortKey(value);
    }

    /**
     * 무작위 알파벳 문자로 채운 placeholder ShortKey.
     * INSERT 시점에 임시로 쓰고, 생성된 id 기반 키로 UPDATE되어 덮어쓰인다.
     * 덮어쓰일 값이라 보안 강도가 필요 없어 {@code ThreadLocalRandom}으로 충분하다.
     */
    public static ShortKey random(Random random) {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return new ShortKey(sb.toString());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShortKey)) return false;
        return value.equals(((ShortKey) o).value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
