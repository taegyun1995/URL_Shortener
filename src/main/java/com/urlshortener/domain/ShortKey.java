package com.urlshortener.domain;

import java.util.Objects;

public final class ShortKey {

    private static final int LENGTH = 7;

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
            if (!Base62Encoder.isValidChar(c)) {
                throw new IllegalArgumentException("ShortKey contains invalid Base62 character: '" + c + "' in: " + value);
            }
        }
        return new ShortKey(value);
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
